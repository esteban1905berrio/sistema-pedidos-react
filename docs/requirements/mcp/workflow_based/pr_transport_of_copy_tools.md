Como desarrollador JAVA quiero crear una nueva tool para generar ordenes de copia.

Requerimiento

**En el sistema giralmcp:**
- existe el progrma ycx_transportar_ot que genera ordenes de copia de un OT.
- Luego cree zclcx_transport_management que es una clase globlar que envuelve los metodos principales del programa anterior.

Quiero que llevar la clase zclcx_transport_management al sistema gdcmcp y envolverla en un FM que estara dentro del GF ZGFCX_1, es FM sera el que llamara la nueva tool y recibira como parametro el numero o numeros de la OT que generara la orden de copia. tambien debe recibir la descipcion de la OT, que por defecto sera la descripcion de la OT padre con el PREFIJO "COPIA:" validar la lonfitud de la descripcion no supere macimos permitido.

Te doy un ejemplo de como debe funcionar la Tool: usuario dice, quiero crear una orden copia del ricefew FIAAC001 el agente busca OT y sus tareas, con esa descripcion, muestra posibles OTs al usuario, usuario escoje y agente llama tool.

Otro ejemplo, crear una orden copia de la Orden CADK911511, el agente debe buscar la OT y las tareas para llamar tool.

Es importante aclarar que la tool solo ba a funcionar con las tareas que son las e contienen los obetos. PEro analiza si la clase ABAP ya no hace la logica de consultar tareas a partir de la OT.