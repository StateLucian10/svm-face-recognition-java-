# SVM Face Recognition in Java

A complete face detection and recognition system implemented from scratch in Java, using Support Vector Machines (SVM) with a Sigmoid kernel, Histogram of Oriented Gradients (HOG) feature extraction, and the Sequential Minimal Optimization (SMO) algorithm.

## Features

- **Face Detection** — Sliding window with image pyramid and Non-Maximum Suppression (NMS)
- **HOG Feature Extraction** — Dalal & Triggs (2005) with L2-Hys normalization
- **SMO Algorithm** — Sequential Minimal Optimization with Sigmoid kernel, from scratch
- **One-vs-All Classification** — One SVM classifier per person
- **Live Webcam Recognition** — 10 FPS real-time detection and recognition
- **Training Data Collection** — Automatic capture via webcam using `detectLargestHead()`
- **Image Viewer** — Browse and delete training images

---

## Project Structure

```
svm-face-recognition-java/
├── compilare.BAT
├── start.BAT
└── svm/
    ├── face/
    │   ├── HOG.java
    │   ├── FaceDetector.java
    │   ├── SlidingWindow.java
    │   └── ImageUtils.java
    ├── model/
    │   ├── SVMModel.java
    │   └── TrainingData.java
    ├── alg/
    │   └── SMO.java
    ├── webcam/
    │   ├── WebcamCapture.java
    │   ├── FaceRecognizer.java
    │   ├── DataCollector.java
    │   └── ImageViewer.java
    ├── face_detector.model
    ├── face_detector_hog.dat
    └── face_models_hog.dat
```

---

## Requirements

- Java JDK 8+
- OpenCV 4.9.0 (`opencv-490.jar` + `opencv_java490.dll`)

## How to Run

```bat
compilare.BAT
start.BAT
```

---

## Architecture & Documentation

### Package `face`

#### `HOG.java`
Implements the Histogram of Oriented Gradients algorithm (Dalal & Triggs, 2005). Extracts an 8100-dimensional feature vector from a 128×128 image.

| Attribute | Type | Description |
|-----------|------|-------------|
| `cellSize` | `int` | Cell size in pixels (default: 8 → 8×8 cell) |
| `blockSize` | `int` | Block size in cells (default: 2 → 2×2 block) |
| `nbins` | `int` | Number of orientation bins (default: 9, covering 0°–180°) |
| `EPS` | `double` | Epsilon for numerical stability in L2 normalization (1e-6) |

| Method | Return | Description |
|--------|--------|-------------|
| `extract(int[][], int, int)` | `float[]` | Extracts HOG vector from a 2D pixel array |
| `extract(int[], int, int)` | `float[]` | Overload for 1D pixel array (BufferedImage format) |
| `toGrayscale(...)` | `float[][]` | Converts RGB to grayscale using ITU-R BT.601 luminance |
| `computeGradients(...)` | `void` | Computes gradients with centered filter [-1, 0, 1] |
| `computeCellHistograms(...)` | `float[][][]` | Builds orientation histograms per cell with bilinear soft binning |
| `normalizeAndConcatenate(...)` | `float[]` | L2-Hys normalization and concatenation into final vector |
| `getVectorSize(int, int)` | `int` | Returns the HOG vector size for a given image size |

**Algorithm steps:**
1. Convert image to grayscale
2. Compute horizontal and vertical gradients with filter [-1, 0, 1]
3. Compute gradient magnitude and orientation per pixel
4. Divide image into 8×8 pixel cells
5. Build orientation histogram per cell (9 bins, 0°–180°, unsigned)
6. Group cells into 2×2 blocks, apply L2-Hys normalization
7. Concatenate all normalized block histograms → 8100-dim vector

---

#### `ImageUtils.java`
Static utility class for image processing. All operations are implemented in pure Java without specialized libraries.

| Method | Return | Description |
|--------|--------|-------------|
| `load(String)` | `BufferedImage` | Loads image from file (JPG, PNG, BMP, GIF) |
| `save(BufferedImage, String)` | `boolean` | Saves image to disk |
| `resize(BufferedImage, int, int)` | `BufferedImage` | Bilinear interpolation resize |
| `crop(BufferedImage, int, int, int, int)` | `BufferedImage` | Crops a rectangular region with automatic clipping |
| `getPixels(BufferedImage)` | `int[]` | Extracts pixels as 1D row-major int array |
| `loadAndExtractHOG(...)` | `float[]` | Pipeline: load + resize + HOG |
| `cropResizeHOG(...)` | `float[]` | Pipeline: crop + resize + HOG (for sliding window) |
| `r/g/b(int)` | `int` | Extracts R/G/B channel from packed int pixel |
| `isImageFile(String)` | `boolean` | Checks extension: jpg, jpeg, png, bmp, gif |

---

#### `SlidingWindow.java`
Implements sliding window detection with an image pyramid. Redundant detections are eliminated with Non-Maximum Suppression (NMS) using Intersection over Union (IoU).

| Attribute | Type | Description |
|-----------|------|-------------|
| `winW, winH` | `int` | Detection window size (128×128 pixels) |
| `stepSize` | `int` | Sliding step in pixels (default: 48) |
| `scaleFactor` | `double` | Scale reduction factor between pyramid levels (default: 2.0) |
| `nmsThresh` | `double` | IoU threshold for NMS — overlaps above this are suppressed (default: 0.1) |

| Method | Description |
|--------|-------------|
| `detect(BufferedImage, Classifier, HOG)` | Runs sliding window on image pyramid, returns detections after NMS |
| `nonMaxSuppression(List<Detection>)` | Greedy NMS: sort by score, suppress overlapping detections |
| `iou(Detection, Detection)` | Computes IoU = intersection_area / union_area |
| `getLargest(List<Detection>)` | Returns detection with maximum area |
| `getBest(List<Detection>)` | Returns detection with maximum classifier score |

---

#### `FaceDetector.java`
Trains and uses the binary SVM classifier for head detection. Implements requirements 1 and 2.

| Attribute | Type | Description |
|-----------|------|-------------|
| `hog` | `HOG` | HOG instance with Dalal & Triggs parameters (cellSize=8, blockSize=2, nbins=9) |
| `sw` | `SlidingWindow` | 128×128 window, stepSize=48, scaleFactor=2.0, NMS=0.1 |
| `model` | `SVMModel` | Trained SVM model for face/non-face binary classification |
| `DEFAULT_MODEL_PATH` | `String` | `svm/face_detector.model` |
| `DEFAULT_HOG_PATH` | `String` | `svm/face_detector_hog.dat` |

| Method | Description |
|--------|-------------|
| `train(posDir, negDir, negPerImg, modelPath, hogPath)` | Trains SVM on positive (faces) and negative (random patches) images |
| `loadModel(String)` | Loads pre-trained model from binary file |
| `detectLargestHead(BufferedImage)` | Detects all heads and returns the 128×128 crop with maximum area (requirement 2) |
| `detectAll(BufferedImage)` | Detects all heads using SlidingWindow, returns list of detections |
| `isReady()` | Returns true if model is loaded and ready |

---

### Package `alg`

#### `SMO.java`
Implements the Sequential Minimal Optimization algorithm (Platt, 1998) with Sigmoid kernel. Solves the SVM dual optimization problem from scratch. Supports both GUI (thread) and standalone (no GUI) modes.

**Sigmoid kernel:**
```
K(x, z) = tanh(γ · ⟨x, z⟩ + coef0)
```

| Attribute | Type | Description |
|-----------|------|-------------|
| `C` | `double` | Regularization — penalizes classification errors |
| `gamma` | `double` | Scale of dot product in sigmoid kernel (default: 0.001) |
| `coef0` | `double` | Free term in sigmoid kernel (default: -1.0) |
| `tol` | `double` | KKT conditions tolerance (default: 0.001) |
| `maxIter` | `long` | Maximum number of SMO iterations |
| `alpha[]` | `double[]` | Lagrange multipliers — solution to the dual problem |
| `b` | `double` | Bias of the separating hyperplane |
| `errors[]` | `double[]` | Cache for classification errors: errors[i] = f(xᵢ) - yᵢ |

| Method | Description |
|--------|-------------|
| `createStandalone(C, γ, coef0, tol, maxIter)` | Factory method — creates SMO without GUI |
| `train(Vector[])` | Main training loop: alternates examineAll and active support vector passes |
| `kernel(int, int, Vector[])` | Computes K(xᵢ, xⱼ) = tanh(γ · ⟨xᵢ, xⱼ⟩ + coef0) |
| `examineExample(int, Vector[])` | Checks KKT condition, finds second multiplier, optimizes pair (αᵢ, αⱼ) |
| `classify(Vector[])` | Computes f(x) = Σ αᵢ·yᵢ·K(xᵢ, x) + b |
| `score(float[])` | Returns decision function score for a given HOG vector |

---

### Package `model`

#### `SVMModel.java`
Stores parameters of a trained SVM classifier with manual serialization via `DataOutputStream`/`DataInputStream`. Supports saving multiple models with HOG vectors in a single file (requirement 6).

| Method | Description |
|--------|-------------|
| `score(float[])` | Computes f(x) = Σ αᵢ · labelᵢ · K(svᵢ, x) + b |
| `save(String)` | Manual serialization: dimensions, alpha, b, labels, support vectors |
| `load(String)` | Manual deserialization from binary file |
| `saveAllWithHOG(SVMModel[], TrainingData, String)` | Saves N models + HOG training vectors in a single file (requirement 6) |
| `loadAllWithHOG(String)` | Loads N models + HOG vectors from a single file |
| `pruneSuportVectors(double)` | Removes support vectors with α < threshold for faster inference |

#### `TrainingData.java`
Stores HOG vectors and labels for training. Dynamic array with capacity doubling and manual serialization.

| Attribute | Type | Description |
|-----------|------|-------------|
| `N` | `int` | Number of stored examples |
| `dim` | `int` | HOG vector dimension (set on first `add()`) |
| `X[][]` | `float[][]` | HOG vectors |
| `y[]` | `int[]` | Labels: y[i] = +1 or -1 |

| Method | Description |
|--------|-------------|
| `add(float[], int)` | Adds example; doubles capacity if full |
| `grow()` | Doubles internal arrays and copies existing data |
| `addAll(TrainingData)` | Adds all examples from another dataset |
| `toVectors()` | Converts to `io.Vector[]` format for SMO |
| `save(String)` | Binary format: `[int N][int dim][int*N labels][float*N*dim vectors]` |
| `load(String)` | Deserialization from binary file |

---

### Package `webcam`

#### `WebcamCapture.java`
Captures images from the webcam using OpenCV `VideoCapture`. **The only class in the project using OpenCV**, exclusively for video capture. BGR→RGB conversion is done in pure Java without `Imgproc`.

| Method | Description |
|--------|-------------|
| `loadOpenCV()` | Loads native OpenCV library once (System.loadLibrary) |
| `open()` | Opens camera and sets resolution/FPS |
| `close()` | Closes camera and releases OpenCV resources |
| `captureFrame()` | Captures frame and converts BGR→RGB in pure Java |
| `captureFrameWithDelay()` | Captures frame and waits for FPS interval |
| `matToBufferedImageBGR(Mat)` | Converts Mat CV_8UC3 BGR → BufferedImage RGB without Imgproc |

#### `DataCollector.java`
Collects training images for face recognition (requirement 3). Uses `detectLargestHead()` and saves 128×128 images with format `nickname_YYYYMMDD_HHmmss_SSS.jpg`.

| Method | Description |
|--------|-------------|
| `collect(String)` | Collects N images for one person using `detectLargestHead()` per frame |
| `collectMultiple(String[], long)` | Collects for multiple persons consecutively |
| `stop()` | Stops collection (`volatile boolean` — thread-safe) |
| `countImages(String)` | Counts saved images for a person |
| `getPersonNames()` | Returns nicknames from subdirectories of rootDir |

#### `ImageViewer.java`
Allows browsing and deleting training images (requirement 4). Extends `java.awt.Dialog`. Uses `java.awt.List` explicitly to avoid conflict with `java.util.List`.

| Method | Description |
|--------|-------------|
| `buildUI()` | Builds GUI: person list, image panel, navigation/delete buttons |
| `populatePersonList()` | Populates list with subdirectories and image counts |
| `showImage(int)` | Displays image at given index, resized to 256×256 |
| `deleteCurrentImage()` | Deletes current image with confirmation dialog |

#### `FaceRecognizer.java`
Trains per-person classifiers and performs live face recognition (requirements 6, 7, 8). One-vs-all strategy: current person's images → label +1, all others → label -1. Drawing done with OpenCV `Imgproc` (permitted by condition 3).

| Attribute | Type | Description |
|-----------|------|-------------|
| `personModels[]` | `SVMModel[]` | Per-person SVM classifiers |
| `running` | `volatile boolean` | Thread-safe flag for stopping live recognition |
| `MODELS_PATH` | `String` | `svm/face_models_hog.dat` |
| `GREEN` | `Scalar` | BGR color (0, 255, 0) for OpenCV drawing |

| Method | Description |
|--------|-------------|
| `trainAll(String)` | One-vs-all training for all persons; saves models + HOG in single file |
| `loadModels(String)` | Loads models from combined binary file |
| `startLive()` | Live recognition: detect → green box → HOG → score → name |
| `startLiveAsync()` | Starts live recognition in a daemon thread |
| `stopLive()` | Stops recognition (`running = false`) |
| `bufferedImageToMat(BufferedImage)` | Converts Java image to OpenCV Mat for drawing |
| `matToBufferedImage(Mat)` | Converts OpenCV Mat back to BufferedImage for GUI |

---

## Training Parameters

### Face Detector (SMO)
| Parameter | Value |
|-----------|-------|
| C | 1.0 |
| gamma | 0.001 |
| coef0 | -1.0 |
| tolerance | 0.001 |
| maxIter | 10,000 |
| pruneThreshold | 0.01 |

### Face Recognizer (SMO)
| Parameter | Value |
|-----------|-------|
| C | 1.0 |
| gamma | 0.001 |
| coef0 | -1.0 |
| tolerance | 0.001 |
| maxIter | 100,000 |
| pruneThreshold | 0.01 |
| recognitionThreshold | 0.6 |

---

## OpenCV Usage

OpenCV is used **only** for webcam capture and drawing, as required by the project conditions:

| Class | OpenCV Usage | Justification |
|-------|-------------|---------------|
| `WebcamCapture` | `VideoCapture.read(Mat)` | Webcam frame capture — explicitly permitted |
| `WebcamCapture` | `mat.get(0, 0, bgrData)` | Extract bytes from Mat for Java conversion |
| `FaceRecognizer` | `Imgproc.rectangle()` | Draw green bounding boxes — explicitly permitted |
| `FaceRecognizer` | `Imgproc.putText()` | Draw person name above box — explicitly permitted |
| `FaceRecognizer` | `mat.put(0, 0, pixels)` | Transfer data to Mat for drawing |

All other algorithms (HOG, SMO, sliding window, NMS, bilinear resize, crop, grayscale) are implemented entirely in pure Java.

---

## Execution Flow

### Training the Face Detector
1. Load positive images (faces) from `positives/`, extract 8100-dim HOG vector for each
2. Extract random patches from negative images (`negatives/`)
3. Train SMO with Sigmoid kernel (C=1.0, γ=0.001, maxIter=10,000)
4. Prune support vectors with α < 0.01
5. Save to `svm/face_detector.model` and `svm/face_detector_hog.dat`

### Collecting Training Images
1. Start webcam (320×240, 10 FPS)
2. For each frame, call `detectLargestHead()` which runs sliding window
3. If a head is detected, save 128×128 image as `nickname_YYYYMMDD_HHmmss_SSS.jpg`
4. Repeat until 500 images saved or Stop pressed

### Training Per-Person Classifiers
1. For each subdirectory in `training_data/`:
   - Person's images → label +1
   - All other persons' images → label -1
   - Train SMO (C=1.0, maxIter=100,000, pruneThreshold=0.01)
2. Save all models + HOG vectors to `svm/face_models_hog.dat`

### Live Face Recognition
1. Webcam captures frame at 10 FPS
2. `detectAll()` runs sliding window with image pyramid
3. Keep detection with highest score (`getBest`) if score > -0.80
4. `Imgproc.rectangle()` draws green bounding box
5. Extract HOG from detected region
6. Each classifier scores the HOG: if score > 0.6, `Imgproc.putText()` writes name
7. Annotated frame sent to GUI via `FrameListener`

---


