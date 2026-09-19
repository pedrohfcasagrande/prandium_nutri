-- V016: Feature `findNutritionist` disponivel apenas no CLIENT_PLUS (paywall na busca de nutris).
-- Atualiza os JSONs de features sem tocar nos demais campos.

UPDATE plans
   SET features = JSON_SET(features, '$.findNutritionist', TRUE)
 WHERE code = 'CLIENT_PLUS';

UPDATE plans
   SET features = JSON_SET(features, '$.findNutritionist', FALSE)
 WHERE code = 'CLIENT_FREE';
