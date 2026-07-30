-- Keep the oldest row if legacy data contains duplicate favorites.
DELETE FROM favorites
WHERE favorite_id NOT IN (
    SELECT retained.favorite_id
    FROM (
        SELECT MIN(favorite_id) AS favorite_id
        FROM favorites
        GROUP BY user_id, product_id
    ) retained
);

ALTER TABLE favorites
    ADD CONSTRAINT ux_favorite_user_product UNIQUE (user_id, product_id);
