cmake_minimum_required(VERSION 3.4.1)

# 拼音引擎核心（静态库）
add_library(pinyin_engine STATIC
    share/dictbuilder.cpp
    share/dictlist.cpp
    share/dicttrie.cpp
    share/lpicache.cpp
    share/matrixsearch.cpp
    share/mystdlib.cpp
    share/ngram.cpp
    share/pinyinime.cpp
    share/searchutility.cpp
    share/spellingtable.cpp
    share/spellingtrie.cpp
    share/splparser.cpp
    share/sync.cpp
    share/userdict.cpp
    share/utf16char.cpp
    share/utf16reader.cpp
)

# JNI 桥接（动态库）
add_library(jni_pinyinime SHARED
    android/com_android_inputmethod_pinyin_PinyinDecoderService.cpp
    share/pinyinime.cpp
    share/matrixsearch.cpp
    share/dicttrie.cpp
    share/dictlist.cpp
    share/dictbuilder.cpp
    share/lpicache.cpp
    share/mystdlib.cpp
    share/ngram.cpp
    share/searchutility.cpp
    share/spellingtable.cpp
    share/spellingtrie.cpp
    share/splparser.cpp
    share/sync.cpp
    share/userdict.cpp
    share/utf16char.cpp
    share/utf16reader.cpp
)

target_include_directories(jni_pinyinime PRIVATE
    ${CMAKE_SOURCE_DIR}/include
)

target_link_libraries(jni_pinyinime
    android
    log
    z
)
