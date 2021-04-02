#!/bin/bash

# Basic configuration
raspi-config nonint do_hostname crowpi

# Change localization options
raspi-config nonint do_change_locale en_US.UTF-8
raspi-config nonint do_configure_keyboard ch
raspi-config nonint do_change_timezone Europe/Zurich

# Enable remote management
raspi-config nonint do_ssh 0
raspi-config nonint do_vnc 0

# Enable additional interfaces
raspi-config nonint do_i2c 0
raspi-config nonint do_spi 0

# Enable WiFi by default
for file in /var/lib/systemd/rfkill/*:wlan; do
  echo 0 > "${file}"
done

# Change default account passwords
echo 'root:crowpi' | chpasswd
echo 'pi:crowpi' | chpasswd

# Install software packages
export DEBIAN_FRONTEND=noninteractive
apt-get -qqy update
apt-get -qqy install \
  git \
  imagemagick \
  maven \
  openjdk-11-jdk \

# Copy custom resources into appropriate locations
sudo -u pi install -Dm 0644 /tmp/resources/pcmanfm-desktop.conf /home/pi/.config/pcmanfm/LXDE-pi/desktop-items-0.conf
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper-autostart.desktop /home/pi/.config/autostart/fhnw-wallpaper.desktop
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper-systemd.service /home/pi/.config/systemd/user/fhnw-wallpaper.service
sudo -u pi install -Dm 0644 /tmp/resources/wallpaper-systemd.path /home/pi/.config/systemd/user/fhnw-wallpaper.path

install -Dm 0644 /tmp/resources/wpa-supplicant.conf /etc/wpa_supplicant/wpa_supplicant.conf
install -Dm 0755 /tmp/resources/wallpaper-hook.sh /lib/dhcpcd/dhcpcd-hooks/99-fhnw
install -Dm 0644 /tmp/resources/wallpaper-static.jpg /opt/fhnw/wallpaper-static.jpg

# Disable getting started wizard
rm /etc/xdg/autostart/piwiz.desktop
