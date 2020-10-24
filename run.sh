nohup java -Dfile.encoding=UTF-8 -classpath bin:fastjson-1.2.62.jar:stanford-postagger.jar main.Json2conllPar output_F > log 2>&1 & 
tail -f log
