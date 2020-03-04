drop view VIEW_DATPRUCHUDAL_LAST;
drop view VIEW_DATPRUCHUDAL_LAST_ID;
drop view VIEW_DATPRUCHUDAL_TODAY;
drop view VIEW_DATPRUCHUDAL;
drop view VIEW_DATZAMEST;


#drop table AIS_USERNAME;
#drop table AIS_WORK;

create table AIS_USERNAME (
   OSCISLO int,
   UZIVJMENO  varchar(255) COLLATE 'cp1250_general_ci',
   ROLE_ADMIN tinyint(1),
   ROLE_HIP tinyint(1),
   FOND int
);

insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (33, 'stieberova', 1, 0, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (41, 'hala', 0, 1, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (84, 'cejka', 0, 0, 80);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (152, 'plzakova', 1, null, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (62, 'meduna', 1, 1, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (90007, 'vrany', 0, 1, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (166, 'dolejs', 0, 1, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (142, 'culik', 0, 1, null);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (171, 'benes', 0, 0, 0);
insert into AIS_USERNAME(OSCISLO, UZIVJMENO) values (20, 'sochorova');
insert into AIS_USERNAME(OSCISLO, UZIVJMENO, ROLE_ADMIN, ROLE_HIP, FOND) values (33, 'stieberova', 1, 0, null);


create table AIS_WORK (
   ID bigint NOT NULL AUTO_INCREMENT,
   DATE date NOT NULL,
   USERNAME varchar(255) NOT NULL,
   HOURS double,
   WORK_TYPE varchar(50) NOT NULL,
   HOURS2 double,
   WORK_TYPE2 varchar(50) NOT NULL,
   WORK_DAY_TYPE varchar(50) NOT NULL,
   PRIMARY KEY (ID)
);

CREATE UNIQUE INDEX idx_ais_username_uq
ON AIS_USERNAME(UZIVJMENO);

create view VIEW_DATZAMEST as
select dz.ID, dz.OSCISLO, dz.JMENO, dz.ID_STREDISKO, u.UZIVJMENO, u.ROLE_ADMIN, u.ROLE_HIP, u.FOND  from DATZAMEST dz left join AIS_USERNAME u on (u.OSCISLO=dz.OSCISLO)
where u.UZIVJMENO is not null and (u.FOND != 0 OR u.FOND is null)
union
select dz.ID, dz.OSCISLO, dz.JMENO, dz.ID_STREDISKO, u.JMENO, 0, null, null from DATZAMEST dz left join USER u on (u.OSC=dz.OSCISLO)
where u.JMENO is not null and dz.OSCISLO not in (select OSCISLO from AIS_USERNAME);

create view VIEW_DATPRUCHUDAL as
select d.id, d.TMKOD as CHIP_CODE, d.ETIME, d.PRIZTYP as ACTION_TYPE, u.JMENO as PERSON_NAME, u.OSCISLO as PERSON_CODE, u.UZIVJMENO as USERNAME, 
s.JMENO AS DEPARTMENT_CODE, p.JMENO as ACTION_NAME, day(d.ETIME) as DAY, month(d.ETIME) as MONTH, year(d.ETIME) as YEAR,
u.ROLE_ADMIN, u.ROLE_HIP, u.FOND
from DATPRUCHUDAL d 
left join ZAMTMKOD z on (z.TMKOD=d.TMKOD) 
left join VIEW_DATZAMEST u on (u.ID=z.ID_ZAMEST)
left join DATSTREDISKO s on (s.ID=u.ID_STREDISKO)
left join DATPRIZN p on (p.ID=d.PRIZNAK)
--left join USER us on (us.
where d.ETIME > '2020-01-01 00:00:00.0' 
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

ALTER TABLE AIS_WORK ADD DESCRIPTION varchar(1000);

