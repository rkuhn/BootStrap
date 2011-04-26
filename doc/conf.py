# -*- coding: utf-8 -*-
#
# BootStrap documentation build configuration file.
#

import sys, os

# -- General configuration -----------------------------------------------------

sys.path.append(os.path.abspath('_sphinx/exts'))
extensions = ['sphinx.ext.todo', 'includecode']

templates_path = ['_templates']
source_suffix = '.rst'
master_doc = 'index'
exclude_patterns = ['_build']

project = u'BootStrap'
copyright = u'2011 Roland Kuhn'
version = '1.0'
release = '1.0'

pygments_style = 'simple'
highlight_language = 'scala'
add_function_parentheses = False
show_authors = True

# -- Options for HTML output ---------------------------------------------------

html_theme = 'nature'
html_theme_options = {
    }
#html_theme_path = ['_sphinx/themes']

html_title = 'BootStrap Documentation'
html_logo = '_sphinx/static/logo.png'
#html_favicon = None

html_static_path = ['_sphinx/static']

html_last_updated_fmt = '%b %d, %Y'
#html_sidebars = {}
#html_additional_pages = {}
html_domain_indices = False
html_use_index = False
html_show_sourcelink = False
html_show_sphinx = False
html_show_copyright = True
htmlhelp_basename = 'BootStrap'

# -- Options for LaTeX output --------------------------------------------------

latex_paper_size = 'a4'
latex_font_size = '10pt'

latex_documents = []

latex_elements = {
    'classoptions': ',oneside,openany',
    'babel': '\\usepackage[english]{babel}',
    'preamble': '\\definecolor{VerbatimColor}{rgb}{0.935,0.935,0.935}'
    }

# latex_logo = '_sphinx/static/akka.png'