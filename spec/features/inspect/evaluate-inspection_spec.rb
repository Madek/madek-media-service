describe "inspection", type: :feature do

  context 'as a system admin, ' do

    include_context :signed_in_as_a_system_admin

    context 'a configured inspector' do
      include_context :inspector

      context 'uploaded JPG image' do
        include_context :uploaded_jpg

        example 'is sucessfully evaluated ' do
          expect(current_url).to match '/media-service/originals/(.+)'
          upload_id = current_url.match(/.*originals\/(.*)/)[1]
          inspection = Inspection.where(media_file_id: upload_id).first
          expect(inspection).to be
          wait_until(60) do
            ['finished'].include? inspection.reload[:state]
          end
          # the propertis of the MediaFile have been updated accordingly
          mf = MediaFile.where(id: inspection.media_file_id).first
          expect(mf).to be
          expect(mf[:content_type]).to eq 'image/jpeg'
          expect(mf[:media_type]).to eq 'image'
          expect(mf[:extension]).to eq 'jpg'
          expect(mf[:height]).to eq 341
          expect(mf[:width]).to eq 606
        end

      end

    end

  end

end

