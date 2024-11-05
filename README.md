# Bachelor's degree project
[![DOI](https://zenodo.org/badge/820957413.svg)](https://doi.org/10.5281/zenodo.14039135)

## Description
This project aims to create a Java application to detect and follow some ArUco markers attached to small robots, in order to give them 
orders to make them move in the field of view of a webcam positioned above them, which will then interpret the markers and calculate the next move.

You can use the repo [Bachelor-s-thesis-report](https://github.com/LorenzoBacchini/Bachelor-s-thesis-report.git) as a guide throught the process that led to the creation of this project,
there you can also find some explanation about architectural and implementation aspects.

## How to
> [!WARNING]
> If the program doesn't seem to work or doesn't show anything, change the selectedCamera variable to use the right index camera.
### Marker detection
Use the released app V1.0.

### Calibration and pose
To perform camera calibration and see the result on the camera pose you can use the second release (V2.0) but you need to:
1. Insert images taken from your webcam of a chessboard into a folder in your pc.
2. Specify the folder path with your images right after launching the app, in the first window.
3. Modify the CHECKBOARD parameter squaresX and squaresY in the first window to match your checkboard size (use only traditional checkboard, not aruco or charuco checkboard).
4. After sending the parameters you have to wait to the start of the program (no more than 10 or 20 seconds).

### Testing
You can print the images in the [aruco_markers](aruco_markers) folder to see the result of the calibration and pose.

> [!WARNING]
> Make sure there is enough light in the scene otherwise the markers may not be detected.
> 
> If you want to be able to use this program in low light conditions you have to modify the CAMERA_EXPOSURE constant at the top of the CameraPose class, increasing the value will allow you to work in low light conditions but will also increase the motion blur effect, while lowering it will have the opposite effect, keep the value between -5 and -7 for correct system operation.

> [!NOTE]
> In the cameraPose class are provided different methods to obtain the pose of the markers and retrieve their position and rotation vectors.


## Robot
In the robot folder there are three subfolder, one for the code of the basic LidarBot (LidarBot), one for the custom code of the LidarBot2 (Lidarbot2) that receive command only through http request, and one for the remote controller of the LidarBot2 (Remote).

> [!IMPORTANT]
> The code in the Robot folder is not tested so LidarBot2 code may not work properly and Remote only work with a stock version of the LidarBot2 code, not with the code in the LidarBot2 folder.
> 
> The code in LidarBot is made for the first version of the lidarBot so may not work with LidarBot2.
> 
> If you want to make any change you can use vscode with PlatformIO extension to open the three subfolder.
