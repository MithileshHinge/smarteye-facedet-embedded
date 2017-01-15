import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.video.ConverterFactory;

public class Main extends Thread {

	private static CascadeClassifier frontal_face_cascade;
	static int frame_no = 0;
	static boolean detectFace = true;

	private static final String outputFilename = "//Users//mithileshhinge//Desktop//videos";
	public static IMediaWriter writer;
	public static boolean store = false;
	public static long startTime;
	public static Date dNow;
	public static SimpleDateFormat ft = new SimpleDateFormat("yyyy_MM_dd'at'hh_mm_ss_a");
	public static boolean writer_close = false;
	
	static OutputStream out;

	static long timeNow1, timeNow2;
	static long time3, time4;
	public static boolean j = false;
	public static boolean fdCounterStart = false;
	public static long fdCounterStartTime = 0;
	
	public static BufferedImage sendimg;
	
	public static boolean fakeNotify, fakeWarn, warned1 = false, warned2 = false;
	
	//public static int last_blackCount = 0;
	
	public static int framesRead = 0;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws IOException {

		//Runtime.getRuntime().exec("/home/autofocus.sh " + 0);

		// sending images to android

		// sending notification to android
		NotificationThread t2 = new NotificationThread();
		t2.start();

		VideoCapture capture = new VideoCapture(1);
		//BackgroundSubtractorMOG2 backgroundSubtractorMOG = new BackgroundSubtractorMOG2(333, 16, false);
		BackgroundSubtractorMOG2 backgroundSubtractorMOG = Video.createBackgroundSubtractorMOG2(333, 16, false);

		// load cascade classifier
		frontal_face_cascade = new CascadeClassifier(
				"//Users//mithileshhinge//Desktop//haarcascade_frontalface_alt.xml");
		if (frontal_face_cascade.empty()) {
			System.out.println("--(!)Error loading Front Face Cascade\n");
			return;
		} else {
			System.out.println("Front Face classifier loaded");
		}

		int faceDetectionsCounter = 0;
		boolean noFaceAlert = true;

		if (!capture.isOpened()) {
			System.out.println("Error - cannot open camera!");
			return;
		}

		while (true) {
			Mat camImage = new Mat();
			capture.read(camImage);

			if (camImage.empty()) {
				System.out.println(" --(!) No captured frame -- Break!");
				continue;
			}
			timeNow1 = System.currentTimeMillis();
			// Background subtraction method
			Mat fgMask = new Mat();
			
			if (j) {
				backgroundSubtractorMOG.apply(camImage, fgMask, -1);
				j = false;
			}else backgroundSubtractorMOG.apply(camImage, fgMask, 0);
			
			
			byte[] buff = new byte[(int) (fgMask.total() * fgMask.channels())];
			fgMask.get(0, 0, buff);

			int blackCount = 0;
			for (int i = 0; i < buff.length; i++) {
				if (buff[i] == 0) {
					blackCount++;
				}
			}
			System.out.println("" + (100 * blackCount / buff.length) + "%");
			
			final int blackCountPercent = 100*blackCount/buff.length;
			
			Mat output = new Mat();
			camImage.copyTo(output, fgMask);

			/*
			 * TODO: Discussion to handle lighting change and peephole covering
			 */

			if (blackCountPercent < 97 && framesRead > 333) {
				
				// storing video to outputfilename
				if (store == true) {
					time3 = System.currentTimeMillis();
					time4 = time3;
					writer = ToolFactory.makeWriter(outputFilename + ft.format(dNow) + ".mp4");
					writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, 640, 480);
					startTime = System.nanoTime();
					writer_close = true;
				}
				store = false;
				BufferedImage camimg = MatToBufferedImage(camImage);
				BufferedImage image2 = ConverterFactory.convertToType(camimg, BufferedImage.TYPE_3BYTE_BGR);
				// encode the image to stream #0
				writer.encodeVideo(0, image2, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

				frame_no++;

				// face detection on every third frame
				if (detectFace && frame_no==3 ) {
					frame_no = 0;
					
					System.out.println("Face Detecting now!");
					MatOfRect front_faces = detect(output);

					Mat outputFaces = new Mat();
					output.copyTo(outputFaces);

					for (Rect rect : front_faces.toArray()) {
						Point center = new Point(rect.x + rect.width * 0.5, rect.y + rect.height * 0.5);
						Imgproc.ellipse(camImage, center, new Size(rect.width * 0.5, rect.height * 0.5), 0, 0, 360,
								new Scalar(0, 255, 0), 4, 8, 0);
					}

					if (front_faces.toArray().length > 0) {
						faceDetectionsCounter++;
						if (faceDetectionsCounter == 1){
							fdCounterStartTime = System.currentTimeMillis();
						}
						if (faceDetectionsCounter >= 5) {
							
							fdCounterStartTime = 0;
							faceDetectionsCounter = 0;
							noFaceAlert = false;
							sendimg = matToImage(camImage);
							fakeNotify = true; //NOTIFY
							System.out.println("Face found.............................");
							detectFace = false;
						}
					} else {
						noFaceAlert = true;
					}
					
					if (fdCounterStartTime!=0 && (System.currentTimeMillis() - fdCounterStartTime)/1000 > 8){
						faceDetectionsCounter = 0;
						fdCounterStartTime = 0;
					}
					
					Mat processed_frame = new Mat();
					Imgproc.pyrDown(camImage, processed_frame);
					BufferedImage image = matToImage(processed_frame);
					

					// 4 second waali condition
					// if ((time4-time3)/1000 >4 && approaching >5 &&
					// warn_level1 == true)
					System.out.println((time4-time3)/1000 + " sec");
					if (blackCountPercent < 70 && (time4-time3)/1000 > 3 && noFaceAlert && !warned1 && !warned2) {
						
						// alert 1
						System.out.println("warn level 1.......................");
						time3 = System.currentTimeMillis();
						warned1 = true;
						/*
						 * String bip = "C:\\Users\\Home\\Desktop\\warning.mp3";
						 * Media hit = new Media(bip); MediaPlayer mediaPlayer =
						 * new MediaPlayer(hit); mediaPlayer.play();
						 */
					}

					// 14 seconds wali condition
					if ((time4 - time3) / 1000 > 15 && noFaceAlert && !warned2) {
						sendimg = matToImage(camImage);
						fakeWarn = true;
						warned2 = true;
						noFaceAlert = false;
						detectFace = false;
						System.out.println("warn level 2........................");
					}
				}

			} else {
				dNow = new Date();
				// System.out.println("Current Date: " + ft.format(dNow));
				store = true;
				if (writer_close) {
					writer.close();
					writer_close = false;
					
					if (fakeNotify) {
						NotificationThread.notify = true;
						fakeNotify = false;
					}
					if (fakeWarn) {
						NotificationThread.warn = true;
						fakeWarn = false;
					}
				}

				warned1 = false;
				warned2 = false;
				detectFace = true;
				faceDetectionsCounter =  0;
				frame_no = 0;
				backgroundSubtractorMOG.apply(camImage, fgMask, -1);
			}

			if (framesRead < 350) {
				framesRead++;
			}
			time4 = System.currentTimeMillis();
			timeNow2 = System.currentTimeMillis();
			System.out.println(timeNow2 - timeNow1);
			System.out.println("frmes_read" + framesRead);
			timeNow1 = timeNow2;
		}
	}

	public static MatOfRect detect(Mat inputframe) {
		Mat mRgba = new Mat();
		Mat mGrey = new Mat();
		MatOfRect front_faces = new MatOfRect();
		// MatOfRect side_faces = new MatOfRect();
		inputframe.copyTo(mRgba);
		inputframe.copyTo(mGrey);
		Imgproc.cvtColor(mRgba, mGrey, Imgproc.COLOR_BGR2GRAY);
		Imgproc.equalizeHist(mGrey, mGrey);
		frontal_face_cascade.detectMultiScale(mGrey, front_faces);

		return front_faces;
	}

	private static BufferedImage MatToBufferedImage(Mat frame) {
		int type = 0;
		if (frame.channels() == 1) {
			type = BufferedImage.TYPE_BYTE_GRAY;
		} else if (frame.channels() == 3) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
		WritableRaster raster = image.getRaster();
		DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
		byte[] data = dataBuffer.getData();
		frame.get(0, 0, data);

		return image;
	}

	public static BufferedImage matToImage(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;
	}
}