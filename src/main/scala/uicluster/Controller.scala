package uicluster

// UI
import java.awt.{Color, Graphics2D, Paint}
import java.awt.image.BufferedImage
import javafx.scene.{chart => jfxsc}
import org.jfree.chart.axis.{NumberTickUnit, TickUnits}
import scala.collection.mutable.ListBuffer
import scala.util.control._
import scalafx.embed.swing.SwingFXUtils
import scalafx.event.ActionEvent
import scalafx.scene.chart._
import scalafx.scene.control._
import scalafx.scene.control.Alert._
import scalafx.scene.image.ImageView
import scalafx.scene.layout.AnchorPane
import scalafx.scene.Node
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafxml.core.{NoDependencyResolver, FXMLLoader}
import scalafxml.core.macros.sfxml

// Logic
import breeze.linalg._
import breeze.numerics._
import breeze.stats._
import breeze.plot._
import java.io.File
import stsc.STSC

@sfxml
class Controller(private val root: AnchorPane, private val selectDataset: ChoiceBox[String], private val min: TextField, private val max: TextField, private val dataset: ImageView, private val clusters: ImageView, private val qualities: Label) {
    private var displayedDataset = DenseMatrix.zeros[Double](0, 0)

    selectDataset.value.onChange {
        if (selectDataset.value.value.takeRight(4) == ".csv") {
            val dataset = new File(getClass.getResource("/" + selectDataset.value.value).getPath())
            displayedDataset = breeze.linalg.csvread(dataset)
            showDataset()
        }
    }

    def cluster(event: ActionEvent) {
        var minClusters = toInt(min.text.value).getOrElse(0)
        var maxClusters = toInt(max.text.value).getOrElse(0)
        var ready = true

        if (ready && minClusters > maxClusters) {
            showAlert("Min has to be the minimum", "The minimum number of clusters must be less than the maximum.")
            ready = false
        }

        if (ready && minClusters < 2) {
            showAlert("Min has to be more than 2", "Having less than 2 clusters is not really interesting...")
            ready = false
        }

        if (ready && maxClusters > 10) {
            showAlert("Max has to be less than 10", "We only cluster small datasets in this app!")
            ready = false
        }

        if (displayedDataset.rows == 0) {
            showAlert("Needs a dataset", "Select a dataset before clustering it.")
            ready = false
        }

        if (ready) {
            val (_, clustersQualities, correctClusters) = STSC.cluster(displayedDataset)
            val colors = List(Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.MAGENTA, Color.CYAN, Color.YELLOW)

            val f = Figure()
            f.visible = false
            f.width = clusters.getBoundsInParent().getWidth().toInt
            f.height = clusters.getBoundsInParent().getHeight().toInt
            val p = f.subplot(0)
            p.title = "Clusters"

            p += scatter(displayedDataset(::, 0), displayedDataset(::, 1), {(_:Int) => 0.01}, {(pos:Int) => colors(correctClusters(pos))}) // Display the observations.
            clusters.image = SwingFXUtils.toFXImage(imageToFigure(f), null)
            qualities.text = clustersQualities.mkString(" | ")
        }
    }

    def loadDataset(event: ActionEvent) {
        val fileChooser = new FileChooser {
            title = "Choose dataset"
            extensionFilters.add(new ExtensionFilter("CSV Files", "*.csv"))
        }
        val selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow())
        if (selectedFile != null) {
            val dataset = breeze.linalg.csvread(selectedFile)
            if (dataset.cols > 2) {
                showAlert("Too many dimensions", "There are " + dataset.cols + "in your CSV, we only need 2.")
            } else {
                selectDataset.value = "Select dataset"
                displayedDataset = dataset
                showDataset()
            }
        }
    }

    private def imageToFigure(f: Figure): BufferedImage = {
        val image = new BufferedImage(f.width, f.height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        f.drawPlots(g2d)
        g2d.dispose
        return image
    }

    private def showAlert(header: String, content: String) {
        new Alert(AlertType.Error) {
            title = "Error"
            headerText = header
            contentText = content
        }.showAndWait()
    }

    private def showDataset() {
        val f = Figure()
        f.visible = false
        f.width = dataset.getBoundsInParent().getWidth().toInt
        f.height = dataset.getBoundsInParent().getHeight().toInt
        val p = f.subplot(0)
        p.title = "Dataset"
        p += scatter(displayedDataset(::, 0), displayedDataset(::, 1), {(_:Int) => 0.01}, {(pos:Int) => Color.BLACK}) // Display the observations.
        dataset.image = SwingFXUtils.toFXImage(imageToFigure(f), null)
        qualities.text = ""
    }

    private def toInt(s: String): Option[Int] = {
        try {
            Some(s.toInt)
        } catch {
            case e: Exception => None
        }
    }
}
