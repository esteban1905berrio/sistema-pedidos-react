Como desarrollador JAVA
Quiero implementar una nueva tool
Para avanzar con el proyecto de migracion

vamos a implementar la tool de get_object_in_open_ot, la finalidad es definir si un objeto esta dentro de una ot no liberada o si el objeto esta bloqueado. Para esto vamos a consultar las tablas E071 donde OBJ_NAME contenga completa o parcialmente el nombre del objeto a buscar, validar el campo LOCKFLAG, si tien X esta bloqueado. Luego ir a E070 con el la misma ot recuperada en el paso anterior y validar si TRSTATUS = D o L. 

Debemos mantener los principios de progressive Discovery y Workflow-Based en tanto sea posible

