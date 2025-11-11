from jnius import autoclass
from plyer import gps
from time import sleep
import json
from datetime import datetime
from pathlib import Path

# Archivo donde se guardarán los datos de ubicación
ruta_datos = Path("/storage/emulated/0/fichajes_service.json")

def on_location(**kwargs):
    now = datetime.now()
    registro = {
        "Fecha": now.strftime("%Y-%m-%d"),
        "Hora": now.strftime("%H:%M:%S"),
        "Latitud": kwargs.get("lat"),
        "Longitud": kwargs.get("lon")
    }
    print("Ubicación registrada:", registro)

    data = []
    if ruta_datos.exists():
        with open(ruta_datos, "r") as f:
            data = json.load(f)
    data.append(registro)
    with open(ruta_datos, "w") as f:
        json.dump(data, f, indent=2)

def main():
    print("Servicio GPS iniciado")
    gps.configure(on_location=on_location, on_status=lambda *a: None)
    gps.start(minTime=60000, minDistance=0)

    # Crear notificación visible (requisito de Android)
    PythonService = autoclass('org.kivy.android.PythonService')
    service = PythonService.mService
    service.startForeground(
        1,
        service.buildNotification("Monitoreo activo", "Servicio GPS en ejecución")
    )

    # Mantener el servicio activo
    while True:
        print("Monitoreo activo")
        sleep(3600)  # cada hora