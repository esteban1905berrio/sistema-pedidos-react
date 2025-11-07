Como desarrollador ABAP
Quiero Crear dos SH (Search help) 
Para agregar un matchcode a los campos Z de las estructuras ampliadas

Requirimeintos

- Estructura CI_PROJ

Crear una ayuda de busqueda que consulta la tabla IMPR, campos POSNR, POSID, POST1.
segun la cantidad de puntos en POSID y el nombre del campo desde donde se llama la SH, determinaremos el resultado de la siguinte forma:

Si campo es ZZ_COD_SECTOR_CPI, filtrar los valores de POSID con solo 1 punto, valor sector.
Si campo es ZZ_COD_PROGRAMA_CPI, filtrar los valores de POSID con solo 2 punto, valor programa.


- Estructura CI_PRPS 

Si campo es ZZ_META, filtrar los valores de POSID con solo 4 punto, valor Meta.
Si campo es ZZ_COD_PRODUCTO, filtrar los valores de POSID con solo 5 punto, valor producto.

Criterios de aceptacion:

- La SH debe retornar la descripcion que es el campo POST1
- Basarse en el EF inicial