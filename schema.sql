CREATE TABLE IF NOT EXISTS Account (
    accountId VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    passwordHash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS PlayerProfile (
    profileId VARCHAR(36) PRIMARY KEY,
    accountId VARCHAR(36) NOT NULL,
    avatarUrl VARCHAR(255),
    totalGamesPlayed INT NOT NULL DEFAULT 0,
    isOnline BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_playerprofile_account
        FOREIGN KEY (accountId) REFERENCES Account(accountId)
);

CREATE TABLE IF NOT EXISTS Score (
    scoreId VARCHAR(36) PRIMARY KEY,
    profileId VARCHAR(36) NOT NULL,
    pointsValue INT NOT NULL,
    dateAchieved TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_score_profile
        FOREIGN KEY (profileId) REFERENCES PlayerProfile(profileId)
);
