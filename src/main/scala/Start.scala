import org.apache.spark.sql.SparkSession

class Start {
}

object Start {

  // Inizializzazione di SparkSession
  val spark: SparkSession = SparkSession.builder().appName("HotelApp")
    .config("spark.master", "local") // Esempio: esegui in modalità locale
    .getOrCreate()

  def main(args: Array[String]): Unit = {

    val dataset = spark.read
      .option("header", "true")
      .csv("database/Hotel_Reviews.csv")

    //Function1.eseguiAnalisi(dataset)
    Function2.eseguiAnalisi(dataset)

    """
    // Seleziona la colonna di testo di interesse
    val textColumn = dataset.select("Negative_Review")

    // Esegui la tokenizzazione del testo e il conteggio delle parole
    val wordCounts = textColumn.rdd
      .flatMap(_.toString.split("\\s+")) // Tokenizzazione del testo
      .map(word => (word, 1)) // Assegna un conteggio iniziale di 1 a ciascuna parola
      // Converti il DataFrame in un RDD per utilizzare le funzioni di RDD
      .reduceByKey(_ + _) // Conteggio delle parole

    // Visualizza i risultati del conteggio delle parole
    wordCounts.collect().foreach(println)
    """

    // Terminazione di SparkSession
    spark.stop()

  }
}

