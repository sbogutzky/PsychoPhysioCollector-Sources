source("/Users/simonbogutzky/Entwicklung/projects/bogutzky/repositories/flow-gait-analysis/r-code/scripts/final/functions/hrv-functions.R")

directory.name <- "2014-05-15_09-19-38"

sensor.bd38.hrv <- read.csv(paste("../data/", directory.name, "/sensor_BD38_hrv.txt", sep =""), header = F, na.strings = "", fill = T, skip = 97, col.names = c("NA1", "Time", "RRInterval", "FFTFrequency", "FFTPSD", "ARFrequency", "ARPSD", "NA2", "NA3", "NA4", "NA5"))
sensor.bd38.hrv <- sensor.bd38.hrv[!is.na(sensor.bd38.hrv$RRInterval),]
sensor.bd38.hrv$RRInterval <- sensor.bd38.hrv$RRInterval * 1000
coherence.ratio <- CalculateHeartRhythmCoherenceRatio(sensor.bd38.hrv$Time, sensor.bd38.hrv$RRInterval, .04, .26, .015, .015, .0033, .4, T)

print(coherence.ratio)