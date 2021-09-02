require 'pry'

describe "displaying dashboard", type: :feature do
  before do
    visit "/"
    cookie_value = MadekOpenSession.build_session_value(User.find_by(login: 'adam'))
    page.driver.browser.manage.add_cookie(name: "madek-session", value: cookie_value)
  end

  it "displays it ;)" do
    visit '/media-service/'

    expect(page).to have_css('.navbar', text: 'Adam Admin')
  end
end
