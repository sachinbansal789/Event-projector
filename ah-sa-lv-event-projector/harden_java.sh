#!/bin/sh
set -x
set -e
# Docker build calls this script to harden the image during build.


# Ensure strict ownership and perms.
if [[ -f /usr/bin/github_pubkeys ]]; then
  chown root:root /usr/bin/github_pubkeys
  chmod 0555 /usr/bin/github_pubkeys
fi

# Be informative after successful login.
echo -e "\n\nHardened App container image built on $(date)." > /etc/motd

# Remove existing crontabs, if any.
rm -fr /var/spool/cron
rm -fr /etc/crontabs
rm -fr /etc/periodic

# Remove all but a handful of admin commands.
find /sbin /usr/sbin ! -type d \
  -a ! -name login_duo \
  -a ! -name nologin \
  -a ! -name setup-proxy \
  -a ! -name sshd \
  -a ! -name start.sh \
  -a ! -name adduser \
  -a ! -name addgroup \
  -delete

# Remove world-writable permissions.
find / -xdev -type d -a ! -name tmp -perm +0002 -exec chmod o-w {} +
find / -xdev -type f -perm +0002 -exec chmod o-w {} +

# Remove unnecessary user accounts.
sed -i -r '/^(user|root|sshd|sa)/!d' /etc/group
sed -i -r '/^(user|root|sshd|sa)/!d' /etc/passwd

# Remove interactive login shell for everybody but user.
sed -i -r '/^user:/! s#^(.*):[^:]*$#\1:/sbin/nologin#' /etc/passwd

sysdirs="
  /bin
  /etc
  /lib
  /sbin
  /usr
"

# Remove apk configs.
find $sysdirs -xdev -regex '.*apk.*' -exec rm -fr {} +

# Remove crufty...
find $sysdirs -xdev -type f -regex '.*-$' -exec rm -f {} +


# Ensure system dirs are owned by root and not writable by anybody else.
find $sysdirs -xdev -type d \
  -exec chown root:root {} \; \
  -exec chmod 0755 {} \;

# Remove all suid files.
find $sysdirs -xdev -type f -a -perm +4000 -delete

# Remove other programs that could be dangerous.
find $sysdirs -xdev \( \
  -name hexdump -o \
  -name chgrp -o \
  -name chmod -o \
  -name chown -o \
  -name ln -o \
  -name od -o \
  -name strings -o \
  -name su \
  \) -delete

# Remove init scripts since we do not use them.
rm -fr /etc/init.d
rm -fr /lib/rc
rm -fr /etc/conf.d
rm -fr /etc/inittab
rm -fr /etc/runlevels
rm -fr /etc/rc.conf

# Remove kernel tunables since we do not need them.
rm -fr /etc/sysctl*
rm -fr /etc/modprobe.d
rm -fr /etc/modules
rm -fr /etc/mdev.conf
rm -fr /etc/acpi

# Remove root homedir since we do not need it.
rm -fr /root

# Remove fstab since we do not need it.
rm -f /etc/fstab

# Remove broken symlinks (because we removed the targets above).
find $sysdirs -xdev -type l -exec test ! -e {} \; -delete
