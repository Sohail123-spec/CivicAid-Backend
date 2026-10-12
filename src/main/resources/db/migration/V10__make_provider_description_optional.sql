-- Provider descriptions are optional informational content.
ALTER TABLE providers
ALTER COLUMN description DROP NOT NULL;