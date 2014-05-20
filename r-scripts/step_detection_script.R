directory.name <- "2014-05-15_09-19-38"

sensor.bc98 <- read.csv(paste("../data/", directory.name, "/sensor_BC98.csv", sep =""))
summary(sensor.bc98)
plot(sensor.bc98$Timestamp[(sensor.bc98$Timestamp / 10^3) > 1650 & (sensor.bc98$Timestamp / 10^3) < 1700] / 10^3, sensor.bc98$Gyroscope.Z[(sensor.bc98$Timestamp / 10^3) > 1650 & (sensor.bc98$Timestamp / 10^3) < 1700], type = "l")
#nrow(sensor.bc98) / ((sensor.bc98$Timestamp[nrow(sensor.bc98)] - sensor.bc98$Timestamp[1]) / 10^3)
#as.POSIXct(sensor.bc98$System.Timestamp[nrow(sensor.bc98)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")