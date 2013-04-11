Justify
-------

Instalación
-----------

El único requisito necesario es disponer de una máquina virtual de Java 1.6 o superior y un acceso a Internet para su funcionamiento. Se garantiza que a fecha 27 de octubre de 2011 la aplicación es compatible con el último protocolo de Spotify.

Uso
---

Justify require que poseas una cuenta Premium de Spotify. Es una restricción de la propia Spotify y de todas las librerías, tanto oficiales como open-source.
Recibe 4 parámetros: usuario, contraseña, dirección de Spotify y comando
La dirección puede ser de una pista concreta, un álbum o una lista de reproducción, accesibles desde el propio Spotify a través del menú contextual (botón derecho del ratón) y seleccionando "Copiar URI de Spotify". En el caso de una pista descargará la canción con el formato "<artista> - <titulo>.ogg", en el caso de un álbum descargará todas las canciones en un directorio llamado "<artista> - <titulo>", y en el caso de una lista de reproducción descargará todas las canciones en un directorio llamado "<creador> - <titulo>". Por defecto todas las canciones serán descargadas con la máxima calidad disponible que actualmente son 320kbps. Las pistas contendrán los metadatos asociados y todas estarán en formato OGG.
El comando puede contener tres valores: "download" para descargar la dirección de Spotify ya sea lista, álbum o pista, "download <número>" igual que el anterior pero especificando que la descarga debe comenzar a partir de la pista número "<número>", y "cover" para descargar la carátula asociada a la pista, álbum o lista de reproducción.
El formato de los nombres de pistas y directorios no se puede modificar a menos que se modifique el código y unas constantes para tal efecto. En futuras versiones podrá ser configurable.

¿Por qué?
---------

Desde que se realizó ingeniería inversa del protocolo de Spotify, era cuestión de tiempo que alguien creara un programa que directamente descargase canciones. Los propios creadores de la librería Despotify, desaconsejan la creación de estos programas que pueden dañar a la empresa. Sin embargo yo no estoy de acuerdo. Se sigue necesitando una cuenta Premium para ello y eso siempre acaba benefiando. Los más perjudicados son legalmente las compañías discográficas que ven como el DRM de Spotify no sirve de nada, y 7digital, que no realizará las ventas esperadas. En mi opinión, pagar 10 euros al mes para acceder al servicio y al completo catálogo de Spotify es cantidad más que suficiente y "justificada" para poder escuchar la música que ofrecen donde yo quiera y como quiera, no donde ellos autoricen ni donde su programa funcione.

Para más información acerca del porqué, los autores de Despotify tienen un FAQ muy completo (en inglés): http://despotify.se/faq/

Changelog / Lista de cambios
----------------------------

v0.1
	- Versión inicial

v0.2
	- Adaptado el código para la última versión de Jotify con fecha del 23 de febrero del 2010 y protocolo de Spotify

v0.3
	- Integración de la librería Jotify en el código (actualmente abandonado)
	- Adaptado a la última versión del protocolo de Spotify

v0.4
	- Corregido un fallo al descargar listas de reproducción

v0.5 (por Klaxnek)
	- Adaptado a la última versión del protocolo de Spotify
	- Añadida la opción para descargar carátulas
	- Integración con la librería JAudioTagger para añadir los metadatos asociados a las canciones

v0.6 (por Klaxnek)
	- Cambio de la librería JAudioTagger a JVorbisComment
	- Adaptado a la última versión del protocolo de Spotify
	- Cambio en el nombre de las carátulas por "folder.jpg"

Agradecimientos y créditos
--------------------------

Todo el mérito va para los autores de Despotify (http://despotify.se/), que consiguieron realizar ingeniería inversa del protocolo y realizar una implementación libre. Realmente es fascinante lo que han conseguido y estudiando su código no tengo más que rendirme ante su habilidad. También gran parte del mérito va para Felix Bruns, que ha portado la librería a la plataforma Java y sobre la que descansa Justify.

También tengo que agradecer especialmente a Klaxnek por mantener el código y añadir nuevas funcionalides en sus últimas versiones.

Si te gusta Spotify, paga por él. Yo lo hago.
