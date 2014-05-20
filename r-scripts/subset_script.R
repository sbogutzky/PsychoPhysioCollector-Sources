setwd("~/Entwicklung/projects/bogutzky/repositories/data-collector-android/r-scripts")

directory.name <- "2014-05-19_17-12-43"

scale <- read.csv(paste("../data/", directory.name, "/scale.csv", sep =""))
sensor.bd38 <- read.csv(paste("../data/", directory.name, "/sensor_BD38.csv", sep =""))
sensor.bc98 <- read.csv(paste("../data/", directory.name, "/sensor_BC98.csv", sep =""))

sensor.bd38$Timestamp <- sensor.bd38$Timestamp - sensor.bd38$Timestamp[1]
summary(sensor.bd38)

sensor.bc98$Timestamp <- sensor.bc98$Timestamp - sensor.bc98$Timestamp[1]
summary(sensor.bc98)

for(i in 1:nrow(scale)) {
  if(i == 1) {
    print(paste("1. ", "Start to", as.POSIXct(scale$System.Timestamp.01[i] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
    subset <- sensor.bd38[sensor.bd38$System.Timestamp < scale$System.Timestamp.01[i],]
    print(paste("2. ", as.POSIXct(subset$System.Timestamp[1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(subset$System.Timestamp[nrow(subset)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
    subset.1 <- sensor.bc98[sensor.bc98$System.Timestamp < scale$System.Timestamp.01[i],]
    print(paste("3. ", as.POSIXct(subset.1$System.Timestamp[1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(subset.1$System.Timestamp[nrow(subset.1)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
  } else {
    print(paste("1. ", as.POSIXct(scale$System.Timestamp.02[i - 1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(scale$System.Timestamp.01[i] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
    subset <- sensor.bd38[sensor.bd38$System.Timestamp >= scale$System.Timestamp.02[i - 1] & sensor.bd38$System.Timestamp < scale$System.Timestamp.01[i],]
    print(paste("2. ", as.POSIXct(subset$System.Timestamp[1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(subset$System.Timestamp[nrow(subset)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"))) 
    subset.1 <- sensor.bc98[sensor.bc98$System.Timestamp >= scale$System.Timestamp.02[i - 1] & sensor.bc98$System.Timestamp < scale$System.Timestamp.01[i],]
    print(paste("3. ", as.POSIXct(subset.1$System.Timestamp[1] / 1000, tz = "Europe/Berlin", origin = "1970-01-01"), "to", as.POSIXct(subset.1$System.Timestamp[nrow(subset.1)] / 1000, tz = "Europe/Berlin", origin = "1970-01-01")))
  }
  write.csv(subset, file = paste("../data/", directory.name, "/sensor_BD38", "_subset_", i, ".csv", sep =""), row.names = FALSE)
  print(paste("Wrote: ", "./data/", directory.name, "/sensor_BD38", "_subset_", i, ".csv", sep =""))
  print(nrow(subset) / ((subset$Timestamp[nrow(subset)] - subset$Timestamp[1]) / 1000))
  write.csv(subset.1, file = paste("../data/", directory.name, "/sensor_BC98", "_subset_", i, ".csv", sep =""), row.names = FALSE)
  print(paste("Wrote: ", "./data/", directory.name, "/sensor_BC98", "_subset_", i, ".csv", sep =""))
  print(nrow(subset.1) / ((subset.1$Timestamp[nrow(subset.1)] - subset.1$Timestamp[1]) / 1000))
}

