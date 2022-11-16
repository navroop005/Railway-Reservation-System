SELECT *
FROM
(
    SELECT train_id,doj,sum(number_of_passengers) sum 
    FROM tickets t 
    GROUP BY train_id,doj
) AS r1,
(
    SELECT train_id, doj, number_sl_coaches, available_sl_berths 
    from journey
) AS r2
WHERE r1.train_id = r2.train_id AND 
    r1.doj = r2.doj AND 
    r2.number_sl_coaches * 24 <> r1.sum + r2.available_sl_berths
;

SELECT *
FROM (
    SELECT t.train_id, t.doj, count(b.berth_no)
    FROM booked b, tickets t
    WHERE b.pnr = t.pnr
    GROUP BY b.coach_no, b.berth_no, t.train_id, t.doj
) AS x
WHERE x.count > 2
