distance <- function(lat1, lon1, lat2, lon2) {
  return(6378.388 * acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(lon2 - lon1)))
}

gps.data <- gps.data.bak
gps.data.bak <- gps.data

gps.data$System.Timestamp <- gps.data$System.Timestamp - gps.data$System.Timestamp[1]

gps.data <- gps.data[gps.data$System.Timestamp >= 10 * 60 * 1000 & gps.data$System.Timestamp < 15 * 60 * 1000,]

d <-0

for(i in 1:(nrow(gps.data) -1)) {
  d <- d + distance(gps.data[i,]$Latitude, gps.data[i,]$Longitude, gps.data[i+1,]$Latitude, gps.data[i+1,]$Longitude)
}