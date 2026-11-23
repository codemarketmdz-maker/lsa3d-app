# Cómo agregar señas al léxico

Archivo: `app/src/main/assets/lexicon_lsa.json`

Cada seña tiene:
- `words`: palabras o frases (hasta 4 palabras) que activan la seña. Se comparan sin tildes ni mayúsculas.
- `validated`: `true` SOLO si la validó una persona sorda o intérprete de LSA. La app muestra
  "LSA validada" solo en ese caso; cualquier otra cosa se muestra como animación de prueba.
- `source`: quién la validó / de qué diccionario o corpus viene.
- `keyframes`: lista de `{ "t": milisegundos, "pose": {...} }`.

Ángulos de `pose` (grados; los que no pongas quedan en reposo):
`rPitch`, `rRoll`, `rElbow` (brazo derecho del avatar), `lPitch`, `lRoll`, `lElbow` (izquierdo), `headYaw`.
- Pitch: 0 = brazo colgando, 90 = al frente, 180 = arriba.
- Roll: separar el brazo hacia afuera.
- Elbow: flexión del codo.

## Limitaciones actuales (necesitan trabajo con la comunidad sorda)
- Las manos son esferas: la LSA depende de la configuración de los dedos, así que hace falta
  un modelo 3D con manos riggeadas para señas reales.
- Faltan expresión facial y movimiento de torso, que en LSA son parte de la gramática.
- Una traducción palabra por palabra NO es LSA: la LSA tiene gramática propia.
  Este motor sirve como base técnica; el contenido lingüístico lo tienen que aportar personas sordas.
