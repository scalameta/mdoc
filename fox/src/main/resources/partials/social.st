$!
  Copyright (c) 2016-2017 Martin Donath <martin.donath@squidfunk.com>
  Copyright (c) 2017 Jonas Fonseca <jonas.fonseca@gmail.com>

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to
  deal in the Software without restriction, including without limitation the
  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
  sell copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
  IN THE SOFTWARE.
!$
<div class="md-footer-social">
  $ if (page.properties.("material.social.web")) $
    <a href="$page.properties.("material.social.web")$" class="md-footer-social__link fa fa-globe"></a>
  $ endif $
  $ if (page.properties.("material.social.github")) $
    <a href="$page.properties.("material.social.github")$" class="md-footer-social__link fa fa-github"></a>
  $ endif $
  $ if (page.properties.("material.social.gitlab")) $
    <a href="$page.properties.("material.social.gitlab")$" class="md-footer-social__link fa fa-gitlab"></a>
  $ endif $
  $ if (page.properties.("material.social.bitbucket")) $
    <a href="$page.properties.("material.social.bitbucket")$" class="md-footer-social__link fa fa-bitbucket"></a>
  $ endif $
  $ if (page.properties.("material.social.linkedin")) $
    <a href="$page.properties.("material.social.linkedin")$" class="md-footer-social__link fa fa-linkedin"></a>
  $ endif $
  $ if (page.properties.("material.social.twitter")) $
    <a href="$page.properties.("material.social.twitter")$" class="md-footer-social__link fa fa-twitter"></a>
  $ endif $
  $ if (page.properties.("material.social.facebook")) $
    <a href="$page.properties.("material.social.facebook")$" class="md-footer-social__link fa fa-facebook"></a>
  $ endif $
</div>
