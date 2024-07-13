# Bachelor's degree project
## Description
This project aims to create a Java application to detect and follow some ArUco markers attached to small robots, in order to give them 
orders to make them move in the field of view of a webcam positioned above them, which will then interpret the markers and calculate the next move

## How to
> [!WARNING]
> If the program doesn't seem to work or doesn't show anything, change the selectedCamera variable to use the right current camera
### Marker detection
Use the released app</br>

### Calibration and pose
To perform camera calibration and see the result on the camera pose you need to clone the repository and then:
1. insert images taken from your webcam of a chessboard into python/images. </br>
   Modify the CHECKBOARD parameter of the .py "calibration" file to make it reflect the number of rows and columns of the chessboard used (do not use images of aruco chessboards/boards, they are not recognized)
3. launch the python program inside the python folder called "calibration"
4. insert the calibration parameters obtained from the python program into the fields of the App class: data and data2
you can now launch the App class and see the result of the calibration used in the CameraPose class

in both cases use the images in the aruco_markers folder to see the result
