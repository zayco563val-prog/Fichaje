from kivy.lang import Builder
from kivy.clock import Clock
from kivy.utils import platform
from kivy.core.window import Window
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.properties import StringProperty, ListProperty

from kivymd.app import MDApp
from kivymd.uix.menu import MDDropdownMenu
from kivymd.uix.dialog import MDDialog
from kivymd.uix.button import MDFlatButton

import json
from datetime import datetime
from pathlib import Path
import gspread
from google.oauth2.service_account import Credentials
from plyer import gps

# --- PARA PRUEBAS EN PC ---
if platform != "android":
    Window.size = (400, 700)

# --- KV DESIGN ---
KV = """
ScreenManager:
    SeleccionScreen:
    FichajeScreen:

<SeleccionScreen>:
    name: "seleccion"
    BoxLayout:
        orientation: "vertical"
        padding: 20
        spacing: 30

        MDLabel:
            text: "Selecciona tu nombre"
            halign: "center"
            font_style: "H5"

        MDFlatButton:
            id: btn_nombre
            text: root.nombre_boton
            pos_hint: {"center_x": 0.5}
            on_release: app.abrir_menu()

        MDFlatButton:
            text: "Continuar"
            md_bg_color: app.theme_cls.primary_color
            text_color: 1, 1, 1, 1
            pos_hint: {"center_x": 0.5}
            on_release: app.ir_a_fichaje()

<FichajeScreen>:
    name: "fichaje"
    BoxLayout:
        orientation: "vertical"
        padding: 20
        spacing: 10

        MDLabel:
            id: estado_lbl
            text: "Sin fichaje activo"
            halign: "center"
            font_style: "H6"

        BoxLayout:
            id: botones_box
            orientation: "vertical"
            spacing: 10
            size_hint_y: None
            height: self.minimum_height

        Widget:
"""

# --- CLASES DE PANTALLAS ---
class SeleccionScreen(Screen):
    nombre_boton = StringProperty("Seleccionar")


class FichajeScreen(Screen):
    pass


# --- APLICACIÓN PRINCIPAL ---
class FichajeApp(MDApp):
    registros = ListProperty([])

    from jnius import autoclass

    def iniciar_servicio(self):
        if platform == "android":
            try:
                PythonService = autoclass('org.kivy.android.PythonService')
                PythonService.start("gpsservice", "Servicio GPS en ejecución")
                print("Servicio de monitoreo iniciado.")
            except Exception as e:
                print("Error al iniciar servicio:", e)

    def build(self):
        # Detectar correctamente Android
        self.modo_pc = platform not in ("android", "ios")
        self.tracking = False
        self.pausado = False
        self.dialog = None
        self.monitoreo_event = None
        self.gps_event = None

        self.root = Builder.load_string(KV)

        # Cargar nombres desde Google Sheets
        self.nombres = self.get_nombres_sheet()
        self.menu_items = [
            {"text": n, "viewclass": "OneLineListItem", "on_release": lambda x=n: self.set_nombre(x)}
            for n in self.nombres
        ]
        self.menu = MDDropdownMenu(caller=None, items=self.menu_items, width_mult=4)

        if platform == "android":
            try:
                from android.permissions import request_permissions, Permission
                request_permissions([
                    Permission.ACCESS_FINE_LOCATION,
                    Permission.ACCESS_COARSE_LOCATION
                ])
                gps.configure(on_location=self.on_gps_location, on_status=self.on_gps_status)
            except Exception as e:
                print("Error inicializando GPS:", e)
        return self.root

    # --- NAVEGACIÓN ENTRE PANTALLAS ---
    def ir_a_fichaje(self):
        seleccion = self.root.get_screen("seleccion")
        if seleccion.nombre_boton == "Seleccionar":
            self.mostrar_dialogo("Debes seleccionar un nombre primero.")
            return
        self.root.current = "fichaje"
        self.actualizar_botones_fichaje(["Iniciar Fichaje"], ["iniciar_fichaje"])

    # --- MENÚ DE NOMBRES ---
    def abrir_menu(self):
        seleccion = self.root.get_screen("seleccion")
        if not self.menu.caller:
            self.menu.caller = seleccion.ids.btn_nombre
        self.menu.open()

    def set_nombre(self, nombre):
        self.root.get_screen("seleccion").nombre_boton = nombre
        self.menu.dismiss()

    # --- FICHAJE ---
    def iniciar_fichaje(self, *args):
        self.iniciar_servicio()
        self.tracking = True
        self.pausado = False
        self.root.get_screen("fichaje").ids.estado_lbl.text = "Fichaje en curso..."
        self.mostrar_dialogo("Fichaje iniciado correctamente.")
        self.actualizar_botones_fichaje(["Pausar Fichaje", "Finalizar Fichaje"], ["pausar_fichaje", "finalizar_fichaje"])

        if not self.modo_pc:
            # Iniciar GPS real (esperar permisos)
            Clock.schedule_once(lambda dt: gps.start(minTime=60000, minDistance=0), 3)

        # Mensaje de monitoreo cada hora
        self.monitoreo_event = Clock.schedule_interval(self.mensaje_monitoreo, 3600)

    def pausar_fichaje(self, *args):
        self.pausado = True
        self.root.get_screen("fichaje").ids.estado_lbl.text = "Fichaje pausado"
        self.actualizar_botones_fichaje(["Reanudar Fichaje", "Finalizar Fichaje"], ["reanudar_fichaje", "finalizar_fichaje"])
        self.mostrar_dialogo("Fichaje pausado. Puedes reanudar cuando desees.")

    def reanudar_fichaje(self, *args):
        self.pausado = False
        self.root.get_screen("fichaje").ids.estado_lbl.text = "Fichaje en curso..."
        self.actualizar_botones_fichaje(["Pausar Fichaje", "Finalizar Fichaje"], ["pausar_fichaje", "finalizar_fichaje"])
        self.mostrar_dialogo("Fichaje reanudado.")

    def finalizar_fichaje(self, *args):
        self.tracking = False
        if self.monitoreo_event:
            self.monitoreo_event.cancel()
        if not self.modo_pc:
            gps.stop()

        self.root.get_screen("fichaje").ids.estado_lbl.text = "Fichaje finalizado"
        self.actualizar_botones_fichaje(["Iniciar Fichaje", "Enviar Registros"], ["iniciar_fichaje", "enviar_registros"])
        self.mostrar_dialogo("Fichaje finalizado. Puedes enviar los registros cuando tengas conexión.")
        self.guardar_localmente()

    def actualizar_botones_fichaje(self, textos, funciones):
        box = self.root.get_screen("fichaje").ids.botones_box
        box.clear_widgets()
        for t, f in zip(textos, funciones):
            btn = MDFlatButton(
                text=t,
                md_bg_color=self.theme_cls.primary_color,
                text_color=(1, 1, 1, 1),
                on_release=getattr(self, f)
            )
            box.add_widget(btn)

    def mensaje_monitoreo(self, dt):
        if self.tracking and not self.pausado:
            print("Monitoreo activo")
            self.mostrar_dialogo("Monitoreo activo", auto_dismiss=True)

    # --- GPS ---
    def on_gps_location(self, **kwargs):
        if self.tracking and not self.pausado:
            now = datetime.now()
            seleccion = self.root.get_screen("seleccion")
            registro = {
                "Fecha": now.strftime("%d-%m-%Y"),
                "Nombre": seleccion.nombre_boton,
                "Hora": now.strftime("%H:%M:%S"),
                "Latitud": kwargs.get("lat"),
                "Longitud": kwargs.get("lon"),
            }
            self.registros.append(registro)
            print("Registro guardado:", registro)

    def on_gps_status(self, stype, status):
        print("GPS status:", stype, status)

    # --- GOOGLE SHEETS ---
    def get_nombres_sheet(self):
        try:
            creds_file = Path(__file__).parent / "credentials.json"
            SCOPE = ["https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive"]
            creds = Credentials.from_service_account_file(creds_file, scopes=SCOPE)
            client = gspread.authorize(creds)
            sheet = client.open_by_url(
                "https://docs.google.com/spreadsheets/d/1iLZ0C2qSWxhNCKRLdJ0Lkl-ePfmROI9meGGVaVtgci0/edit#gid=0"
            )
            nombres = sheet.worksheet("Auxiliar").col_values(1)[1:]
            return [n for n in nombres if n.strip()]
        except Exception as e:
            print("Error al obtener nombres:", e)
            return ["Usuario1", "Usuario2"]

    def enviar_registros(self, *args):
        if not self.registros and not Path("registros_offline.json").exists():
            self.mostrar_dialogo("No hay registros para enviar.")
            return

        self.mostrar_dialogo("Enviando registros, por favor espera...", auto_dismiss=False)

        try:
            if Path("registros_offline.json").exists():
                with open("registros_offline.json", "r") as f:
                    self.registros.extend(json.load(f))

            creds_file = Path(__file__).parent / "credentials.json"
            SCOPE = ["https://www.googleapis.com/auth/spreadsheets", "https://www.googleapis.com/auth/drive"]
            creds = Credentials.from_service_account_file(creds_file, scopes=SCOPE)
            client = gspread.authorize(creds)
            sheet = client.open_by_url(
                "https://docs.google.com/spreadsheets/d/1iLZ0C2qSWxhNCKRLdJ0Lkl-ePfmROI9meGGVaVtgci0/edit#gid=0"
            )
            ws = sheet.worksheet("Registro")

            if not ws.get_all_values():
                ws.append_row(["Fecha", "Nombre", "Hora", "Latitud", "Longitud"])

            for r in self.registros:
                ws.append_row([r["Fecha"], r["Nombre"], r["Hora"], r["Latitud"], r["Longitud"]])

            self.registros = []
            Path("registros_offline.json").unlink(missing_ok=True)
            self.mostrar_dialogo("✅ Registros enviados correctamente.", auto_dismiss=True)
        except Exception as e:
            print("Error al enviar:", e)
            self.guardar_localmente()
            self.mostrar_dialogo("Error de conexión. Registros guardados localmente.", auto_dismiss=True)

    # --- GUARDADO LOCAL ---
    def guardar_localmente(self):
        if self.registros:
            with open("registros_offline.json", "w") as f:
                json.dump(self.registros, f, indent=2)
            print("Registros guardados localmente en registros_offline.json")

    # --- UTILIDADES ---
    def mostrar_dialogo(self, mensaje, auto_dismiss=False):
        if self.dialog:
            self.dialog.dismiss()
        self.dialog = MDDialog(title="Aviso", text=mensaje)
        if not auto_dismiss:
            self.dialog.buttons = [MDFlatButton(text="OK", on_release=lambda x: self.dialog.dismiss())]
        self.dialog.open()
        if auto_dismiss:
            Clock.schedule_once(lambda dt: self.dialog.dismiss(), 3)


if __name__ == "__main__":
    FichajeApp().run()