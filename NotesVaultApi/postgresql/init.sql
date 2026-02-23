CREATE TABLE IF NOT EXISTS note (
                                    id UUID PRIMARY KEY,
                                    content VARCHAR(100) NOT NULL,
                                    created_at TIMESTAMP
);