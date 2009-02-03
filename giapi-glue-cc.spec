%define _prefix __auto__
%define gemopt opt
%define name giapi-glue-cc
%define version 0
%define release 5
%define repository gemini

%define debug_package %{nil}

Summary: %{name} Package
Name: %{name}
Version: %{version}
Release: %{release}.%{dist}.%{repository}
License: GPL
## Source:%{name}-%{version}.tar.gz
Group: Gemini
Source0: %{name}-%{version}.tar.gz
BuildRoot: /var/tmp/%{name}-%{version}-root
BuildArch: %{arch}
Prefix: %{_prefix}
## You may specify dependencies here
BuildRequires: apr-devel apr-util-devel apache-log4cxx-devel activemq-cpp-devel
Requires: apr apr-util apache-log4cxx activemq-cpp
## Switch dependency checking off
# AutoReqProv: no

%description
This is a default description for the %{name} package

## If you want to have a devel-package to be generated uncomment the following:
%package devel
Summary: %{name}-devel Package
Group: Development/Gemini
Requires: %{name}
%description devel
This is a default description for the %{name}-devel package

## Of course, you also can create additional packages, e.g for "doc". Just
## follow the same way as I did with "%package devel".

%prep
## Do some preparation stuff, e.g. unpacking the source with
%setup -n %{name}


%build
## Write build instructions here, e.g
# sh configure
# make
make
make install

%install
## Write install instructions here, e.g
## install -D zzz/zzz  $RPM_BUILD_ROOT/%{_prefix}/zzz/zzz
mkdir -p $RPM_BUILD_ROOT/%{prefix}/{include,lib}
cp -a install/lib $RPM_BUILD_ROOT/%{prefix}/
cp -a install/include $RPM_BUILD_ROOT/%{prefix}/

## if you want to do something after installation uncomment the following
## and list the actions to perform:
# %post
## actions, e.g. /sbin/ldconfig

## If you want to have a devel-package to be generated and do some
## %post-stuff regarding it uncomment the following:
# %post devel

## if you want to do something after uninstallation uncomment the following
## and list the actions to perform. But be aware of e.g. deleting directories,
## see the example below how to do it:
# %postun
## if [ "$1" = "0" ]; then
##	rm -rf %{_prefix}/zzz
## fi

## If you want to have a devel-package to be generated and do some
## %postun-stuff regarding it uncomment the following:
# %postun devel

## Its similar for %pre, %preun, %pre devel, %preun devel.

%clean
## Usually you won't do much more here than
rm -rf $RPM_BUILD_ROOT


%files
%defattr(-,root,root)
## list files that are installed here, e.g
## %{_prefix}/zzz/zzz
%{_prefix}/lib

## If you want to have a devel-package to be generated uncomment the following
%files devel
%defattr(-,root,root)
## list files that are installed by the devel package here, e.g
## %{_prefix}/zzz/zzz
%{_prefix}/include


%changelog
## Write changes here, e.g.
# * Thu Dec 6 2007 John Doe <jdoe@gemini.edu> VERSION-RELEASE
# - change made
# - other change made
