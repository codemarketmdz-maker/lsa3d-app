# LSA 3D app

Proyecto Android inicial de CodeMarket para accesibilidad en Lengua de Señas Argentina (LSA).

## Estado real de esta versión
- Modo Misa con inicio y detención de escucha.
- Reconocimiento de voz en español mediante el servicio disponible en Android.
- Transcripción visible en pantalla.
- Selector de avatar femenino/masculino como preferencia visual.
- La app informa claramente cuando una frase todavía no tiene una traducción LSA validada.

**Esta versión todavía NO traduce frases libres a LSA ni reproduce señas 3D animadas.** No se inventan señas. Para implementar esa etapa se necesita un corpus de LSA validado por personas sordas argentinas y especialistas, además de modelos y animaciones de señas.

## Compilar
El workflow está en `.github/workflows/build.yml`.
1. Subí el contenido de este ZIP a la raíz del repositorio `codemarketmdz-maker/lsa3d-app`.
2. Abrí la pestaña **Actions**.
3. Elegí **Compilar LSA 3D app** y ejecutá el workflow, o esperá que se ejecute al subir los archivos.
4. Descargá el artefacto `LSA3DApp-debug-apk` al terminar correctamente.

El APK generado es de depuración (debug), instalable para pruebas. No es una versión firmada de distribución.

## Requisitos
- Android 7.0 (API 24) o superior.
- Permiso de micrófono.
- La disponibilidad del reconocimiento offline depende del motor de voz instalado en el teléfono.

Paquete: `com.codemarket.lsa3d`.
