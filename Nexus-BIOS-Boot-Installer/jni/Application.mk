# Copyright (C) 2011, SNDA
# ndk application configuration for 7za
# Author: tangyaguang@snda.com
APP_ABI := armeabi armeabi-v7a

# Add stl port support, this setting is not automatically added
APP_STL := gnustl_static

APP_PLATFORM := android-19

APP_CFLAGS += -Wno-error=format-security