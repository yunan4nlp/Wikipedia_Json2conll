package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringReader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;

import java.nio.charset.Charset;

public class Json2conllPar {
	static TokenizerFactory tf = PTBTokenizer.factory(new CoreLabelTokenFactory(), "tokenizePerLine=False,"
			+ "invertible=True," + "strictTreebank3=True," + "splitHyphenated=True," + "normalizeParentheses=True");
	static String REGURL = "(http[s]?:\\/\\/([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))";

	@SuppressWarnings("unchecked")
	public static int parse(String input_path, String output_path) throws Exception {

		BufferedReader inf = new BufferedReader(new InputStreamReader(new FileInputStream(input_path), "UTF-8"));
		BufferedWriter outf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_path), "UTF-8"));
		String line;
		int doc_num = 0;
		while ((line = inf.readLine()) != null) {
			JSONObject obj = JSON.parseObject(line);
			String title = obj.getString("title");
			String id = obj.getString("id");
			String url = obj.getString("url");
			String text = obj.getString("text");
			text = text.replaceAll("<.*?>", " ");
			text = text.replaceAll(REGURL, " URL ");

			title = edu.stanford.nlp.util.StringUtils.normalize(title);
			text = edu.stanford.nlp.util.StringUtils.normalize(text);
			boolean is_ascii = Charset.forName("US-ASCII").newEncoder().canEncode(text);
			int offset = 0;

			boolean long_sentence = false;
			List<List<List<HasWord>>> doc_sentences = new ArrayList<List<List<HasWord>>>();
			for (String str : text.split("\n\n")) {
				str = str.trim();
				if (offset == 0 && str.equals(title))
					continue;
				if (str.equals(""))
					continue;
				offset += 1;
				DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(str));
				dp.setTokenizerFactory(tf);

				List<List<HasWord>> par_sentences = new ArrayList<List<HasWord>>();
				for (List<HasWord> sentence : dp) {
					int ch_len = 0;

					for (HasWord word : sentence) {
						ch_len += word.toString().length();
					}

					// int sent_len = sentence.size();
					if (ch_len > 512) {
						long_sentence = true;
						break;
					}
					par_sentences.add(sentence);
				}
				doc_sentences.add(par_sentences);
			}

			if (!long_sentence && doc_sentences.size() > 0 && is_ascii) {

				doc_num += 1;
				System.out.println("# newdoc id = " + id + "### title = " + title + "### url = " + url);
				outf.write("# newdoc id = " + id + "### title = " + title + "### url = " + url);
				outf.newLine();

				for (List<List<HasWord>> par_sentence : doc_sentences) {
					int sentence_id = 0;
					for (List<HasWord> sentence : par_sentence) {
						int word_id = 1;
						for (HasWord word : sentence) {
							String w = word.toString();
							w = w.replaceAll("\\xa0", " ");
							w = w.replaceAll(" ", "");
							String conll_line = "";
							if (word_id == sentence.size()) {
								
								if (sentence_id + 1 == par_sentence.size())
									conll_line = word_id + "\t" + w + "\t_\t_\t_\t_\tParagraph\t_\t_\t_\t";
								else
									conll_line = word_id + "\t" + w + "\t_\t_\t_\t_\tSentence\t_\t_\t_\t";
									
							}							
							else
								conll_line = word_id + "\t" + w + "\t_\t_\t_\t_\t_\t_\t_\t_\t";
							outf.write(conll_line);
							outf.newLine();
							word_id++;
						}
						outf.newLine();
						outf.flush();
						sentence_id += 1;
					}

				}
			}
		}
		inf.close();
		outf.close();
		return doc_num;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Json covert to conll.");
		int doc_num = 0;
		File file = new File(args[0]);
		for (File subf : file.listFiles()) {
			if (!subf.isDirectory())
				continue;
			for (File f : subf.listFiles()) {
				if (!f.isDirectory()) {
					String inFile = f.toString();
					String outFile = f.toString() + ".conll";
					if (inFile.indexOf(".conll") == -1)
						doc_num += parse(inFile, outFile);
				}
			}
		}
		System.out.println("doc num: " + doc_num);
		System.out.println("Json covert OK.");
	}
}
