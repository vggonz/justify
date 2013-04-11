Justify
-------

Instalación
-----------

Justify necesita la librería Jotify descargable desde http://jotify.felixbruns.de/
Se ha probado con la última versión disponible a fecha del 28 de enero del 2010 y descargable directamente desde http://github.com/fxb/jotify/zipball/ac2044119edab3f41fbbf8680341dd37572283e7
No es necesario satisfacer todas las dependencias y se pueden borrar tranquilamente los directorios "gui" y "gateway" de la librería que no son necesarios para el funcionamiento de Justify.

Uso
---

Justify require que poseas una cuenta Premium de Spotify. Es una restricción de la propia Spotify y de todas las librerías, tanto oficiales como open-source.
Recibe 3 parámetros: usuario, contraseña y dirección de Spotify
La dirección puede ser de una pista concreta, un álbum o una lista de reproducción. En el caso de una pista descargará la canción con el formato "<artista> - <titulo>.ogg", en el caso de un álbum descargará todas las canciones en un directorio llamado "<artista> - <titulo>", y en el caso de una lista de reproducción descargará todas las canciones en un directorio llamado "<creador> - <titulo>". Por defecto todas las canciones serán descargadas con la máxima calidad disponible que actualmente son 320kbps. Ninguna pista contendrá metadatos asociados tipo ID3 y todas estarán en formato OGG.

¿Por qué?
---------

Desde que se realizó ingeniería inversa del protocolo de Spotify, era cuestión de tiempo que alguien creara un programa que directamente descargase canciones. Los propios creadores de la librería Despotify, desaconsejan la creación de estos programas que pueden dañar a la empresa. Sin embargo yo no estoy de acuerdo. Se sigue necesitando una cuenta Premium para ello y eso siempre acaba benefiando. Los más perjudicados son legalmente las compañías discográficas que ven como el DRM de Spotify no sirve de nada, y 7digital, que no realizará las ventas esperadas. En mi opinión, pagar 10 euros al mes para acceder al servicio y al completo catálogo de Spotify es cantidad más que suficiente y "justificada" para poder escuchar la música que ofrecen donde yo quiera y como quiera, no donde ellos autoricen ni donde su programa funcione.

Además, no distribuyo binarios y delega todo lo posible en la librería Jotify, siguiendo con la doble moral de los autores de Despotify, que publican su código fuente pero no la documentación sobre cómo funciona el protocol de Spotify.

Para más información acerca del porqué, los autores de Despotify tienen un FAQ muy completo (en inglés): http://despotify.se/faq/

Agradecimientos y créditos
--------------------------

Todo el mérito va para los autores de Despotify (http://despotify.se/), que consiguieron realizar ingeniería inversa del protocolo y realizar una implementación libre. Realmente es fascinante lo que han conseguido y estudiando su código no tengo más que rendirme ante su habilidad. También gran parte del mérito va para Felix Bruns, que ha portado la librería a la plataforma Java y sobre la que descansa Justify.

Si te gusta Spotify, paga por él. Yo lo hago.
