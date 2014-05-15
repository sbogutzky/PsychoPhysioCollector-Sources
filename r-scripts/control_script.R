directory.name <- "2014-05-15_09-19-38"

sensor_BC98 <- read.csv(paste("../data/", directory.name, "/sensor_BC98.csv", sep =""))
summary(sensor_BC98)
plot(sensor_BC98$Timestamp)
nrow(sensor_BC98) / ((sensor_BC98$Timestamp[nrow(sensor_BC98)] - sensor_BC98$Timestamp[1]) / 10^3)
as.POSIXct(sensor_BC98$System.Timestamp[nrow(sensor_BC98)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")

sensor_BD38 <- read.csv(paste("../data/", directory.name, "/sensor_BD38.csv", sep =""))
summary(sensor_BD38)
plot(sensor_BD38$Timestamp)
nrow(sensor_BD38) / ((sensor_BD38$Timestamp[nrow(sensor_BD38)] - sensor_BD38$Timestamp[1]) / 10^3)
as.POSIXct(sensor_BD38$System.Timestamp[nrow(sensor_BD38)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")

accelerometer <- read.csv(paste("../data/", directory.name, "/accelerometer.csv", sep =""))
summary(accelerometer)
plot(accelerometer$Timestamp)
nrow(accelerometer) / ((accelerometer$Timestamp[nrow(accelerometer)] - accelerometer$Timestamp[1]) / 10^9)
as.POSIXct(accelerometer$System.Timestamp[nrow(accelerometer)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")

gyroscope <- read.csv(paste("../data/", directory.name, "/gyroscope.csv", sep =""))
summary(gyroscope)
plot(gyroscope$Timestamp)
nrow(gyroscope) / ((gyroscope$Timestamp[nrow(gyroscope)] - gyroscope$Timestamp[1]) / 10^9)
as.POSIXct(gyroscope$System.Timestamp[nrow(gyroscope)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")

gps <- read.csv(paste("../data/", directory.name, "/gps.csv", sep =""))
summary(gps)
plot(gps$System.Timestamp)
as.POSIXct(gps$System.Timestamp[nrow(gps)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")

scale <- read.csv(paste("../data/", directory.name, "/scale.csv", sep =""))
summary(scale)
plot(scale$System.Timestamp.01)
as.POSIXct(scale$System.Timestamp.02[nrow(scale)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")

