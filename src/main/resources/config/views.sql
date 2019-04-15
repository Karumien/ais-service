create view VIEW_DATPRUCHUDAL as 
select d.id, d.TMKOD as CHIP_CODE, d.ETIME, d.PRIZTYP as ACTION_TYPE, u.JMENO as PERSON_NAME, u.OSCISLO as PERSON_CODE, s.JMENO AS DEPARTMENT_CODE, p.JMENO as ACTION_NAME 
from DATPRUCHUDAL d 
left join ZAMTMKOD z on (z.TMKOD=d.TMKOD) 
left join DATZAMEST u on (u.ID=z.ID_ZAMEST)
left join DATSTREDISKO s on (s.ID=u.ID_STREDISKO)
left join DATPRIZN p on (p.ID=d.PRIZNAK)
--left join USER us on (us.
where d.ETIME > '2019-02-01 00:00:00.0' 
order by d.ETIME desc;


create view VIEW_DATPRUCHUDAL_TODAY as
select * from VIEW_DATPRUCHUDAL
where day(ETIME) = day(now()) and month(ETIME) = month(now()) and year(ETIME) = year(now()) 
order by ETIME desc;


create view VIEW_DATPRUCHUDAL_LAST_ID as
select max(d.id) as id, d.PERSON_CODE from  VIEW_DATPRUCHUDAL_TODAY d
group by d.PERSON_CODE;


create view VIEW_DATPRUCHUDAL_LAST as
select * from VIEW_DATPRUCHUDAL_TODAY t where t.id in ( 
select id from  VIEW_DATPRUCHUDAL_LAST_ID)
order by t.ETIME desc;

