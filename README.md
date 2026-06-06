<div align="center">
<h1>Meld + SpotifyLyrics</h1>
<p>Un cliente de música modificado para Android que envía telemetría de reproducción en tiempo real a SpotifyLyrics.</p>
</div>

## 📌 Objetivo del Proyecto

Este proyecto es una modificación basada en la aplicación de código abierto [Meld](https://github.com/FrancescoGrazioso/Meld) (un cliente de música de Android que unifica Spotify y YouTube Music). 

El objetivo principal de este fork es **extraer la telemetría de reproducción** (cuándo se reproduce una canción, cuándo se pausa, los saltos en la línea de tiempo o "seeks", y los cambios de pista) y enviarla en tiempo real a una aplicación web externa llamada [SpotifyLyrics](https://github.com/victorigp/SpotifyLyrics). 

Gracias a esto, el usuario puede estar escuchando música cómodamente en su móvil mientras una pantalla externa, televisión o monitor muestra automáticamente las letras sincronizadas (estilo karaoke) y el videoclip de la canción de fondo.

## ✨ Características de la Modificación

- **Sincronización en Tiempo Real**: Envío de peticiones HTTP en segundo plano al backend cada vez que el estado del reproductor local cambia.
- **Detección de Seeks (Saltos de tiempo)**: Sistema de *debounce* integrado que detecta cuándo el usuario avanza o retrocede la canción, notificando los milisegundos exactos para que el video web salte al mismo segundo.
- **Heartbeat Activo**: Envío periódico (latido) del progreso de la canción para evitar desincronizaciones por latencia.
- **Funcionamiento Transparente**: La capa de comunicación de red se ejecuta de forma asíncrona mediante corrutinas de Kotlin sin afectar el rendimiento ni la interfaz original del reproductor.

## 🛠️ Modificaciones Técnicas

Se ha inyectado la clase SpotifyLyricsSyncManager directamente en el motor de reproducción (basado en ExoPlayer). Este manager intercepta:
- Cambios de metadatos (Título, Álbum, Artista, Duración).
- Estado de reproducción (IsPlaying / IsPaused).
- Posición absoluta de la barra de progreso.

Los datos se empaquetan en un JSON y se envían a un endpoint POST /api/meld-sync para ser almacenados ultrarrápidamente en una base de datos Redis.

## ⚖️ Licencia y Créditos

Este proyecto se basa íntegramente en el excelente trabajo de [FrancescoGrazioso/Meld](https://github.com/FrancescoGrazioso/Meld).

Hereda y respeta la licencia original **GPL-3.0**, garantizando que el código fuente, incluidas estas modificaciones de telemetría, permanezca libre, abierto y disponible para la comunidad.
