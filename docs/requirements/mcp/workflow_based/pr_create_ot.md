Como desarrollador SAP en gdcmcp
quiero crear una tool que cree OT, Workbench, customizing o de copia
Para hacer mas agil el proceso de desarrollo

Requerimientos

- Primero que todo vamos a investigar opciones en SAP ABAP para crear esto, puede ser con ADT o con FM o clases estandar que podamos agrugar en un wrapper RFC.
- La tool debe recibir el tipo de OT a crear y un listado de objetos a incluir de forma opcional
- Tambien debe recibir la descipcion de la OT
- Puede crearce con referencia a otras OT, heredar su descripcion y objetos
- La tool debe estar en la capacidad de liberar una OT

Criterios de aceptacion

- basarse en codgio actual de zclcx_transport_management
- Crear en ZGFCX_01
- Contemplar OT y tareas
- Mantener la funcionalidad dentro de los services de OT
- Crear Test para que yo lo pueda ejecutar de forma manual segun la regla establesida en el proyecto