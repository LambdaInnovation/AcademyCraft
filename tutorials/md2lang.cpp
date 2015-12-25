// md2lang.cpp
// Copyright (c) Lambda Innovation, 2013-2015.
// Converts a dialect of markdown to single-line lang file with tag for in-mod rendering.
// Original code : EAirPeter
// Adaption      : WeAthFolD

#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <string>
#include <locale>
#include <codecvt>
#include <vector>

using cn_string = std::wstring;
using cn_char = wchar_t;

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

void pdefault(cn_string &o, const cn_char *c);
bool pbr(cn_string &o, const cn_char *&c);
bool phx(cn_string &o, const cn_char *&c);
bool pquote(cn_string &o, const cn_char *&c);
bool pem(cn_string &o, const cn_char *&c);
bool pimg(cn_string &o, const cn_char *&c);

inline void pdef(cn_string &o, const cn_char *&c) {
	switch (*c) {
	case '[':
		o += L"[lb]";
		break;
	case ']':
		o += L"[rb]";
		break;
	default:
		o += *c;
	}
	++c;
}

void pdefault(cn_string &o, const cn_char *c) {
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

bool pbr(cn_string &o, const cn_char *&c) {
	if (*c != '\n')
		return false;
	const cn_char *t = c++;
	while (*c == '\n')
		++c;
	if (c - t > 1)
		o += L"[br]";
	return true;
}

bool phx(cn_string &o, const cn_char *&c) {
	static const wchar_t *xhx[6][2] {
		{L"[h1]", L"[/h1]"},
		{L"[h2]", L"[/h2]"},
		{L"[h3]", L"[/h3]"},
		{L"[h4]", L"[/h4]"},
		{L"[h5]", L"[/h5]"},
		{L"[h6]", L"[/h6]"},
	};
	if (*c != '#')
		return false;
	const cn_char *t = c++;
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
	const cn_char **hx = xhx[c - t - 2];
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

bool pquote(cn_string &o, const cn_char *&c) {
	if (*c != '>')
		return false;
	const cn_char *t = c++;
	if (*(c++) != ' ') {
		c = t;
		return false;
	}
	o += L"[quote]";
	while (*c && *c != '\n') {
		if (pem(o, c))
			continue;
		if (pimg(o, c))
			continue;
		pdef(o, c);
	}
	o += L"[/quote]";
	return true;
}

bool pem(cn_string &o, const cn_char *&c) {
	if (*c != '*')
		return false;
	const cn_char *t = c++;
	if (*(c++) != '*') {
		c = t;
		return false;
	}
	cn_string u {L"[em]"};
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
		o += L"[/em]";
		return true;
	}
	c = t;
	return false;
}

bool pimg(cn_string &o, const cn_char *&c) {
	if (*c != '!')
		return false;
	const cn_char *t = c++;
	if (*(c++) != '[') {
		c = t;
		return false;
	}
	cn_string u { L"[img src=\""};
	while (*c && *c != ']')
		u += *(c++);
	if (!*c) {
		c = t;
		return false;
	}
	u += L"\"";
	++c;
	if (*(c++) != '(') {
		c = t;
		return false;
	}
	if (*c == ')') {
		++c;
		o += u;
		o += L"]";
		return true;
	}
	cn_string v {};
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
	u += L" [width=";
	u += v;
	v.clear();
	while (*c && *c != '\n' && *c != ')')
		v += *(c++);
	if (!*c || *c == '\n') {
		c = t;
		return false;
	}
	o += u;
	o += L" height=";
	o += v;
	o += L"]]";
	++c;
	return true;
}

#define DLLEXPORT extern "C" __declspec(dllexport)

cn_char buf2[9999];

DLLEXPORT const cn_char* md2lang(const cn_char* input) 
{
	cn_string out{};

	pdefault(out, input);

	const cn_char* cstr = out.c_str();
    
    std::fill(buf2, buf2 + 9999, '\0');
	std::copy(cstr, cstr + out.length(), buf2);
	return buf2;
}