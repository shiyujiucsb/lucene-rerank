
for i in 10 20 50 100
do
  java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 0 -tree $i -leaf 2 -train $1 -metric2t NDCG@20 -save "models/mart_"$i".txt" &
done

#java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 1 -train $1 -metric2t NDCG@20 -save "models/model_ranknet.txt" &

#java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 2 -train $1 -metric2t NDCG@20 -save "models/model_rankboost.txt" &

#java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 3 -train $1 -metric2t NDCG@20 -save "models/model_adarank.txt" &

#java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 4 -train $1 -metric2t NDCG@20 -save "models/model_ca.txt" &

for i in 10 20 50 100
do
  java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 6 -tree $i -leaf 2 -train $1 -metric2t NDCG@20 -save "models/lambda_"$i".txt" &
done 

#java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 7 -train $1 -metric2t NDCG@20 -save "models/model_listnet.txt" &

#java -Xmx32000m -jar ./RankLib-2.5.jar -silent -ranker 8 -train $1 -metric2t NDCG@20 -save "models/model_rf.txt" &


