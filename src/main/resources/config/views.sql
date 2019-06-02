drop view VIEW_DATPRUCHUDAL_LAST;
drop view VIEW_DATPRUCHUDAL_LAST_ID;
drop view VIEW_DATPRUCHUDAL_TODAY;
drop view VIEW_DATPRUCHUDAL;
drop view VIEW_DATZAMEST;

create table AIS_USERNAME (
   OSCISLO int,
   UZIVJMENO  varchar(255) COLLATE 'cp1250_general_ci');

insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (33, 'stieberova');
insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (41, 'hala');
insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (84, 'cejka');
insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (152, 'plzakova');
insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (62, 'meduna');
-- insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (90006, 'sochorova');

create table AIS_WORK (
   ID bigint NOT NULL AUTO_INCREMENT,
   DATE date NOT NULL,
   USERNAME varchar(255) NOT NULL,
   HOURS double,
   WORK_TYPE varchar(50) NOT NULL,
   WORK_DAY_TYPE varchar(50) NOT NULL,
   PRIMARY KEY (ID)
);

CREATE UNIQUE INDEX idx_ais_username_uq
ON AIS_USERNAME(UZIVJMENO);

create view VIEW_DATZAMEST as
select dz.ID, dz.OSCISLO, dz.JMENO, dz.ID_STREDISKO, u.UZIVJMENO  from DATZAMEST dz left join AIS_USERNAME u on (u.OSCISLO=dz.OSCISLO)
where u.UZIVJMENO is not null
union
select dz.ID, dz.OSCISLO, dz.JMENO, dz.ID_STREDISKO, u.JMENO from DATZAMEST dz left join USER u on (u.OSC=dz.OSCISLO)
where u.JMENO is not null and dz.OSCISLO not in (select OSCISLO from AIS_USERNAME);

select d.id, d.TMKOD as CHIP_CODE, d.ETIME, d.PRIZTYP as ACTION_TYPE, u.JMENO as PERSON_NAME, u.OSCISLO as PERSON_CODE, u.UZIVJMENO as USERNAME, 
s.JMENO AS DEPARTMENT_CODE, p.JMENO as ACTION_NAME, day(d.ETIME) as DAY, month(d.ETIME) as MONTH, year(d.ETIME) as YEAR
from DATPRUCHUDAL d 
left join ZAMTMKOD z on (z.TMKOD=d.TMKOD) 
left join VIEW_DATZAMEST u on (u.ID=z.ID_ZAMEST)
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



