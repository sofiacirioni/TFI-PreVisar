-- V016: numero de orden del profesional (complementa la matricula)
-- En CIEC la matricula coincide con el DNI; el numero de orden (4 digitos)
-- es el complemento. Se muestran juntos como "matricula/numero de orden".
ALTER TABLE profesional ADD COLUMN numero_orden VARCHAR(4);