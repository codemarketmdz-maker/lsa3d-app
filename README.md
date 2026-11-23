# LSA 3D app

Proyecto Android inicial de CodeMarket para accesibilidad en Lengua de Señas Argentina (LSA).

## Estado real de esta versión
- Modo Misa: escucha continua (se reinicia sola entre frases) con reconocimiento de voz en español (es-AR).
- Avatar 3D animado (femenino/masculino) del torso superior; se gira arrastrando.
- Motor de traducción: texto -> palabras/frases -> señas del léxico (`app/src/main/assets/lexicon_lsa.json`) -> animación por keyframes.
- Cada palabra se marca como **LSA validada**, **animación de prueba** o **sin seña validada**. No se inventan señas.
- Campo de texto para probar sin micrófono (probá la palabra `demo`).

**Falta (requiere personas sordas / intérpretes de LSA):** el diccionario validado, manos con dedos, expresión facial y gramática propia de la LSA. Ver `docs/AGREGAR_SENAS.md`.

## Compilar
El workflow está en `.github/workflows/main.yml`.
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
