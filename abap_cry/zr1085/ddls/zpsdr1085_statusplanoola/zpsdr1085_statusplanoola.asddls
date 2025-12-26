@AbapCatalog.sqlViewName: 'ZPSDR1085_1'
@AbapCatalog.compiler.compareFilter: true
@AbapCatalog.preserveKey: true
@AccessControl.authorizationCheck: #CHECK
@EndUserText.label: 'Status Plano Ola'
define view ZPSDR1085_STATUSPLANOOLA
  as select from nast
{
  key objky        as entrega,
  key max( erdat ) as fecha,
  key max( eruhr ) as hora,
  min(
      case
          when vstat = '0' then //Amarillo
            '@09@'
          when vstat = '1' then //Verde
            '@08@'
          when vstat = '2' then //rojo
            '@0A@'
          else
            '@EB@'
          end      
      ) as status_plano_ola
}
where
  nast.kschl = 'ZPKM'
group by
  objky
