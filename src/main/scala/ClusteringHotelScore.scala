import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Encoder, Encoders, SparkSession}

class ClusteringHotelScore {
}

object ClusteringHotelScore{

  private def nation(s: String) = {
    val list_splitted: Array[String] = s.split(" ")
    if (list_splitted.nonEmpty)
        list_splitted.last
    else " "
  }


  def main(args: Array[String]){
    val inputFile = "C:\\Users\\asus\\Desktop\\progetto_big_data\\Hotel_Reviews.csv"
    val spark = SparkSession.builder.master("local[*]").appName("HotelReviewsAnalysis").getOrCreate()


    val dati: DataFrame = spark.read
      .option("header", "true") // Se la prima riga è l'intestazione
      .option("inferSchema", "true") // Inferisci automaticamente il tipo di dati delle colonne
      .csv(inputFile)

    val dati_distinti_per_hotel: DataFrame= dati.dropDuplicates("Hotel_Address")

    val assembler = new VectorAssembler()
      .setInputCols(Array("Average_Score"))
      .setOutputCol("features")

    val assembledScores = assembler.transform(dati_distinti_per_hotel)


    val kmeans = new KMeans().setK(3).setSeed(1L).setPredictionCol("prediction")
    val model = kmeans.fit(assembledScores)

    val dataFrameClassified: DataFrame = model.transform(assembledScores)


    // Fornisci un implicit Encoder per il tuo tipo di dato
    implicit val encoder: Encoder[(String, Int)] = Encoders.product[(String, Int)]

    val RDD_Nazione_LEVEL : RDD[(String, Int)]= dataFrameClassified.select("Hotel_Address", "prediction").
      map(row => (nation(row.getAs[String]("Hotel_Address")), row.getAs[Int]("prediction")))(encoder).rdd

    val RDD_Nazione_CountHotel: RDD[(String, Int)]= RDD_Nazione_LEVEL.map { case (chiave, _) => (chiave, 1)}.reduceByKey((a, b)=> a+b)

    val RDD_Nazione_GOOD: RDD[(String, Int)]= RDD_Nazione_LEVEL
      .filter( coppia=> coppia._2==2)
      .map {case(chiave, _) =>(chiave, 1)}
      .reduceByKey(_+_)

    val RDD_Nazione_INTERMEDIATE: RDD[(String, Int)] = RDD_Nazione_LEVEL
      .filter(coppia => coppia._2 == 1)
      .map { case (chiave, _) => (chiave, 1) }
      .reduceByKey(_ + _)

    val RDD_Nazione_BAD: RDD[(String, Int)] = RDD_Nazione_LEVEL
      .filter(coppia => coppia._2 == 0)
      .map { case (chiave, _) => (chiave, 1) }
      .reduceByKey(_ + _)

    val RDD_Nazione_Percent_GOOD = RDD_Nazione_GOOD.join(RDD_Nazione_CountHotel).map{case (chiave, (sum, count)) => (chiave, sum.toDouble /count*100)}
    val RDD_Nazione_Percent_INTERMEDIATE = RDD_Nazione_INTERMEDIATE.join(RDD_Nazione_CountHotel).map{case (chiave, (sum, count)) => (chiave, sum.toDouble /count*100)}
    val RDD_Nazione_Percent_BAD = RDD_Nazione_BAD.join(RDD_Nazione_CountHotel).map{case (chiave, (sum, count)) => (chiave, sum.toDouble /count*100)}


    println("---Percentuale hotel buoni per nazione---")
    RDD_Nazione_Percent_GOOD.foreach(println)

    println("---Percentuale hotel intermedi per nazione---")
    RDD_Nazione_Percent_INTERMEDIATE.foreach(println)

    println("---Percentuale hotel scarsi per nazione---")
    RDD_Nazione_Percent_BAD.foreach(println)

    //Mostra i centroidi dei cluster
    println("Cluster Centers: ")
    model.clusterCenters.foreach(println)
  }
}