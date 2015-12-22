// md2lang.cpp
// Copyright (c) Lambda Innovation, 2013-2015.
// Converts a dialect of markdown to single-line lang file with tag for in-mod rendering.
// Original code: EAirPeter
// Adaption     : WeAthFolD

#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>

std::FILE  *fin = nullptr;
std::FILE  *fou = nullptr;

char        buf[8192] = {};

inline void assert_(bool val, const char *msg) {
    if (!val) {
        std::fputs(msg, stderr);
        std::fputc('\n', stderr);
        std::abort();
    }
}

void pdefault(std::string &o, const char *c);
bool pbr(std::string &o, const char *&c);
bool phx(std::string &o, const char *&c);
bool pquote(std::string &o, const char *&c);
bool pem(std::string &o, const char *&c);
bool pimg(std::string &o, const char *&c);

inline void pdef(std::string &o, const char *&c) {
    switch (*c) {
    case '[':
        o += "[lb]";
        break;
    case ']':
        o += "[rb]";
        break;
    default:
        o += *c;
    }
    ++c;
}

void pdefault(std::string &o, const char *c) {
    if (phx(o, c) || pquote(o, c) || true)
        while (*c) {
            if (pbr(o, c) && (phx(o, c) || pquote(o, c) || true)) {
                continue;
                if (phx(o, c))
                    continue;
                if (pquote(o, c))
                    continue;
                continue;
            }
            if (pem(o, c))
                continue;
            if (pimg(o, c))
                continue;
            pdef(o, c);
        }
}

bool pbr(std::string &o, const char *&c) {
    if (*c != '\n')
        return false;
    const char *t = c++;
    while (*c == '\n')
        ++c;
    if (c - t > 1)
        o += "[br]";
    return true;
}

bool phx(std::string &o, const char *&c) {
    static const char *xhx[6][2] {
        {"[h1]", "[/h1]"},
        {"[h2]", "[/h2]"},
        {"[h3]", "[/h3]"},
        {"[h4]", "[/h4]"},
        {"[h5]", "[/h5]"},
        {"[h6]", "[/h6]"},
    };
    if (*c != '#')
        return false;
    const char *t = c++;
    while (*c == '#')
        ++c;
    if (c - t > 6) {
        c = t;
        return false;
    }
    if (!*c || *(c++) != ' ') {
        c = t;
        return false;
    }
    const char **hx = xhx[c - t - 2];
    o += hx[0];
    while (*c && *c != '\n') {
        if (pem(o, c))
            continue;
        if (pimg(o, c))
            continue;
        pdef(o, c);
    }
    o += hx[1];
    return true;
}

bool pquote(std::string &o, const char *&c) {
    if (*c != '>')
        return false;
    const char *t = c++;
    if (*(c++) != ' ') {
        c = t;
        return false;
    }
    o += "[quote]";
    while (*c && *c != '\n') {
        if (pem(o, c))
            continue;
        if (pimg(o, c))
            continue;
        pdef(o, c);
    }
    o += "[/quote]";
    return true;
}

bool pem(std::string &o, const char *&c) {
    if (*c != '*')
        return false;
    const char *t = c++;
    if (*(c++) != '*') {
        c = t;
        return false;
    }
    std::string u {"[em]"};
    bool res = false;
    while (*c && *c != '\n') {
        if (*c == '*') {
            if (*(++c) == '*') {
                ++c;
                res = true;
                break;
            }
            else
                --c;
        }
        if (pimg(u, c))
            continue;
        pdef(u, c);
    }
    if (res) {
        o += u;
        o += "[/em]";
        return true;
    }
    c = t;
    return false;
}

bool pimg(std::string &o, const char *&c) {
    if (*c != '!')
        return false;
    const char *t = c++;
    if (*(c++) != '[') {
        c = t;
        return false;
    }
    std::string u {"[img src=\""};
    while (*c && *c != ']')
        u += *(c++);
    if (!*c) {
        c = t;
        return false;
    }
    u += "\"";
    ++c;
    if (*(c++) != '(') {
        c = t;
        return false;
    }
    if (*c == ')') {
        ++c;
        o += u;
        o += "]";
        return true;
    }
    std::string v {};
    while (*c && *c != '\n' && *c != ',' && *c != ')')
        v += *(c++);
    if (!*c || *c == '\n') {
        c = t;
        return false;
    }
    if (*(c++) == ')') {
        c = t;
        return false;
    }
    u += " [width=";
    u += v;
    v.clear();
    while (*c && *c != '\n' && *c != ')')
        v += *(c++);
    if (!*c || *c == '\n') {
        c = t;
        return false;
    }
    o += u;
    o += " height=";
    o += v;
    o += "]]";
    ++c;
    return true;
}

#define DLLEXPORT extern "C" __declspec(dllexport)


extern "C" {
    const __stdcall char* md2lang(const char* input) 
    {
        std::string out {};
        pdefault(out, input);
        return out.c_str();
    }
}
