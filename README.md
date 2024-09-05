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
1. insert images taken from your webcam of a chessboard into a folder in your pc. </br>
2. specify the folder path you chose in the directoryPath attribute inside the App class 
   Modify the CHECKBOARD parameter markersX and markersY in the App class to match your checkboard size (use only traditional checkboard, not aruco or charuco checkboard)
3. Now you can launch the main inside the App class, you will see a list of images path in processing in the terminal, in order to return camera parameters, then the pose program will start,
   showing you a canvas with the image captured by the camera and a pose estimation of the markers detected

you can use the images in the aruco_markers folder to see the result

> [!WARNING]
> Make sure there is enough light in the scene otherwise the markers may not be detected


## Robot
In the robot folder there are three subfolder, one for the code of the basic LidarBot (LidarBot), one for the custom code of the LidarBot2 (Lidarbot2) that receive command only through http request, and one for the remote controller of the LidarBot2 (Remote)

> [!IMPORTANT]
> At the moment this folder is work in progress so LidarBot2 code may not work properly and Remote only work with a stock version of the LidarBot2 code, not with the code in the LidarBot2 folder.
> If you want to make any change you can use vscode with PlatformIO extension to open the three subfolder
