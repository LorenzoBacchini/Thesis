import cv2
import numpy as np

def draw_axes(frame, rvec, tvec, camera_matrix, dist_coeffs, marker_length) :                
    # Definisci gli assi 3D
    axis = np.float32([[0, 0, 0],
                    [marker_length, 0, 0],
                    [0, marker_length, 0],
                    [0, 0, -marker_length]]).reshape(-1, 3)
    # Proietta i punti 3D sugli assi 2D dell'immagine
    projected_points, _ = cv2.projectPoints(axis, rvec, tvec, camera_matrix, dist_coeffs)
    # Converti i punti proiettati in un formato utilizzabile da cv2.line
    pts = projected_points.reshape(-1, 2).astype(int)
    # Disegna gli assi sull'immagine
    cv2.line(frame, tuple(pts[0]), tuple(pts[1]), (0, 0, 255), 2)  # X asse in rosso
    cv2.line(frame, tuple(pts[0]), tuple(pts[2]), (0, 255, 0), 2)  # Y asse in verde
    cv2.line(frame, tuple(pts[0]), tuple(pts[3]), (255, 0, 0), 2)  # Z asse in blu


def calculate_reprojection_error(obj_points, img_points, rvecs, tvecs, camera_matrix, dist_coeffs):
    total_error = 0
    total_points = 0

    for i in range(len(obj_points)):
        img_points_proj, _ = cv2.projectPoints(obj_points[i], rvecs[i], tvecs[i], camera_matrix, dist_coeffs)
        
        # Assicurati che img_points e img_points_proj abbiano lo stesso tipo di dati
        img_points_proj = img_points_proj.reshape(-1, 2)
        img_points[i] = img_points[i].reshape(-1, 2)
        
        if img_points_proj.shape != img_points[i].shape:
            print(f"Dimensioni diverse rilevate. img_points_proj: {img_points_proj.shape}, img_points[i]: {img_points[i].shape}")
            continue

        error = cv2.norm(img_points[i], img_points_proj, cv2.NORM_L2) / len(img_points_proj)
        total_error += error
        total_points += len(img_points_proj)

    return total_error / total_points if total_points > 0 else 0


# Definisci il dizionario dei marker ArUco
aruco_dict = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_4X4_50)
parameters = cv2.aruco.DetectorParameters()
detector = cv2.aruco.ArucoDetector(aruco_dict, parameters)
# Carica la matrice della camera e i coefficienti di distorsione
# (Sostituisci con i tuoi valori ottenuti dalla calibrazione)
camera_matrix = np.array([[1.34481689e+03, 0.00000000e+00, 9.45455782e+02],
                          [0.00000000e+00, 1.34485743e+03, 5.71671069e+02],
                          [0.00000000e+00, 0.00000000e+00, 1.00000000e+00]])
dist_coeffs = np.array([-3.66002754e-01, 8.58545102e-03, -8.16420804e-05, -1.02155112e-03, 2.81500718e-01])
# Definisci la lunghezza del lato del marker in metri
marker_length = 0.07
# Calcola i punti oggetto per il marker 4x4
obj_points = np.array([[-marker_length/2, marker_length/2, 0],
                       [marker_length/2, marker_length/2, 0],
                       [marker_length/2, -marker_length/2, 0],
                       [-marker_length/2, -marker_length/2, 0]], dtype=np.float32)
img_points = []
rvecs = []
tvecs = []
# Apri la webcam
cap = cv2.VideoCapture(1)
while True:
    ret, frame = cap.read()
    if not ret:
        break
    # Converti l'immagine in scala di grigi
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    # Rileva i marker ArUco
    corners, ids, rejected = detector.detectMarkers(gray)
    if ids is not None:
        # Stima la posa dei marker
        for i in range(len(ids)):
            # Converti i punti dell'immagine in MatOfPoint2f
            image_points = np.array(corners[i][0], dtype=np.float32).reshape(-1, 2)
            img_points.append(image_points)
            # Calcola la posa del marker usando solvePnP
            success, rvec, tvec = cv2.solvePnP(obj_points, image_points, camera_matrix, dist_coeffs, flags=cv2.SOLVEPNP_IPPE_SQUARE)
            if success:
                rvecs.append(rvec)
                tvecs.append(tvec)
                # Disegna il bordo del marker
                cv2.aruco.drawDetectedMarkers(frame, corners)
                # Disegna l'asse sul marker
                #cv2.drawFrameAxes(frame, camera_matrix, dist_coeffs, rvec, tvec, marker_length)
                draw_axes(frame, rvec, tvec, camera_matrix, dist_coeffs, marker_length)
                # Stampa i vettori di rotazione e traslazione per debug
                print(f"rvec: {rvec}")
                print(f"tvec: {tvec}")
                # Calcola la distanza del marker dalla camera
                
                distance = np.linalg.norm(tvec)/2
                #print(f"Marker ID: {ids[i][0]} - Distanza: {distance:.2f} m")
                # Visualizza la distanza sull'immagine
                cv2.putText(frame, f"ID: {ids[i][0]} Dist: {distance:.2f} m",
                            (int(corners[i][0][0][0]), int(corners[i][0][0][1]) - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
    # Mostra il frame
    cv2.imshow('ArUco Marker Detection', frame)
    # Esci dal loop premendo 'q'
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break
cap.release()
cv2.destroyAllWindows()

# Calcola l'errore di riproiezione
if img_points and rvecs and tvecs:
    reprojection_error = calculate_reprojection_error([obj_points] * len(img_points), img_points, rvecs, tvecs, camera_matrix, dist_coeffs)
    print(f"Errore di riproiezione: {reprojection_error:.4f}")