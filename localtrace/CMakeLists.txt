cmake_minimum_required(VERSION 3.4.1)

project("localTrace")

find_package(shadowhook REQUIRED CONFIG)
# 添加你需要构建的库
add_library(localTrace SHARED
        src/main/cpp/hook.cpp)  # 你的源代码文件

target_compile_options(localTrace PUBLIC  -Wno-c++98-compat-pedantic -Wno-zero-as-null-pointer-constant -Wno-old-style-cast)
target_include_directories(localTrace PUBLIC .)
set_target_properties(localTrace PROPERTIES LINK_FLAGS "${ARCH_LINK_FLAGS}")

find_library(log-lib log)  # 引入日志库
target_link_libraries(localTrace ${log-lib} shadowhook::shadowhook)  # 链接日志库






