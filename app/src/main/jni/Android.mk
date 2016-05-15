##
## Copyright (C) 2016  Michael Roland <mi.roland@gmail.com>
##
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <http://www.gnu.org/licenses/>.
##

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

BASE_PATH := $(LOCAL_PATH)
LIBUSB_ROOT := $(BASE_PATH)/libusb01
LIBNFC_ROOT := $(BASE_PATH)/libnfc

include $(LIBUSB_ROOT)/Android.mk
include $(LIBNFC_ROOT)/Android.mk
include $(BASE_PATH)/mfoc/Android.mk
include $(BASE_PATH)/mfcuk/Android.mk
